package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.llm.LlmTurnResult;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.model.ReplyLength;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.SendMessageResponse;
import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.model.World;
import com.aditya.roleplay.storage.JsonStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class RoleplayService {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    @Inject
    ConversationService conversationService;

    @Inject
    CharacterService characterService;

    @Inject
    PlayerPersonaService personaService;

    @Inject
    StoryService storyService;

    @Inject
    JsonStorageService storage;

    @Inject
    PromptService promptService;

    @Inject
    RoleplayLlmService roleplayLlmService;

    @Inject
    StateChangeProcessor stateChangeProcessor;

    @Inject
    StoryStateService storyStateService;

    @Inject
    RelationshipService relationshipService;

    public SendMessageResponse processTurn(String conversationId, String content) {
        return processTurn(conversationId, content, null, null);
    }

    public SendMessageResponse processTurn(String conversationId, String content, String userApiKey) {
        return processTurn(conversationId, content, userApiKey, null);
    }

    public SendMessageResponse processTurn(
            String conversationId,
            String content,
            String userApiKey,
            String replyLengthValue) {
        validateContent(content);
        Conversation conversation = conversationService.getConversation(conversationId);
        return processTurnInternal(
                conversation,
                content.trim(),
                ReplyLength.fromString(replyLengthValue),
                userApiKey,
                true);
    }

    public SendMessageResponse regenerateLastTurn(
            String conversationId,
            String replyLengthValue,
            String userApiKey) {
        Conversation conversation = conversationService.getConversation(conversationId);
        List<Message> messages = new ArrayList<>(conversation.messages());

        while (!messages.isEmpty() && messages.get(messages.size() - 1).role() == Role.ASSISTANT) {
            messages.remove(messages.size() - 1);
        }

        Message lastUserMessage = null;
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (messages.get(index).role() == Role.USER) {
                lastUserMessage = messages.get(index);
                break;
            }
        }

        if (lastUserMessage == null) {
            throw new RoleplayException("No user message to regenerate from", "INVALID_REQUEST", 400);
        }

        Conversation trimmedConversation = conversation.withMessages(messages);
        return processTurnInternal(
                trimmedConversation,
                lastUserMessage.content(),
                ReplyLength.fromString(replyLengthValue),
                userApiKey,
                false);
    }

    private SendMessageResponse processTurnInternal(
            Conversation conversation,
            String trimmedContent,
            ReplyLength replyLength,
            String userApiKey,
            boolean appendUserMessage) {

        RoleplayCharacter character = characterService.requireCharacter(conversation.characterId());
        World world = characterService.requireWorld(conversation.worldId());
        conversation = ensureRuntimeFields(conversation, character);

        PlayerPersona playerPersona = loadPlayerPersona(conversation).orElse(null);
        Story story = loadStory(conversation).orElse(null);

        Set<String> allowedRelationshipTargets = characterService.allowedRelationshipTargets(
                conversation.worldId(),
                conversation.characterId(),
                conversation.playerPersonaId());

        LlmTurnResult turnResult = roleplayLlmService.generateTurnResult(
                character, world, conversation, trimmedContent,
                allowedRelationshipTargets, playerPersona, story, userApiKey, replyLength);

        Message userMessage = appendUserMessage
                ? new Message(UUID.randomUUID().toString(), Role.USER, trimmedContent, Instant.now())
                : null;

        StateChangeProcessor.ConversationState updatedState = stateChangeProcessor.apply(
                new StateChangeProcessor.ConversationState(
                        conversation.characterId(),
                        conversation.characterState(),
                        conversation.scene(),
                        conversation.relationships(),
                        conversation.events(),
                        conversation.memories()),
                character,
                turnResult,
                allowedRelationshipTargets,
                conversation.resolvedPlayerPersonaId(),
                world);

        Message assistantMessage = new Message(
                UUID.randomUUID().toString(),
                Role.ASSISTANT,
                turnResult.response().trim(),
                Instant.now());

        Conversation updatedConversation = conversation;
        if (userMessage != null) {
            updatedConversation = updatedConversation.appendMessage(userMessage);
        }
        updatedConversation = updatedConversation
                .appendMessage(assistantMessage)
                .withCharacterState(updatedState.characterState())
                .withScene(updatedState.scene())
                .withRelationships(updatedState.relationships())
                .withEvents(updatedState.events())
                .withMemories(updatedState.memories())
                .withUpdatedAt(Instant.now());

        conversationService.save(updatedConversation);

        return new SendMessageResponse(
                assistantMessage,
                updatedConversation.id(),
                updatedState.scene(),
                updatedState.characterState(),
                updatedState.relationships());
    }

    private Optional<PlayerPersona> loadPlayerPersona(Conversation conversation) {
        if (conversation.playerPersonaId() == null || conversation.playerPersonaId().isBlank()) {
            return Optional.empty();
        }
        return storage.loadPersona(conversation.playerPersonaId());
    }

    private Optional<Story> loadStory(Conversation conversation) {
        if (conversation.storyId() == null || conversation.storyId().isBlank()) {
            return Optional.empty();
        }
        return storage.loadStory(conversation.storyId());
    }

    private Conversation ensureRuntimeFields(Conversation conversation, RoleplayCharacter character) {
        if (conversation.characterState() != null
                && conversation.events() != null
                && conversation.memories() != null
                && !conversation.relationships().isEmpty()) {
            return conversation;
        }

        return conversation
                .withCharacterState(conversation.characterState() != null
                        ? conversation.characterState()
                        : storyStateService.createInitialCharacterState(character))
                .withEvents(conversation.events() != null ? conversation.events() : java.util.List.of())
                .withMemories(conversation.memories() != null ? conversation.memories() : java.util.List.of())
                .withRelationships(conversation.relationships().isEmpty()
                        ? relationshipService.createInitialRelationships(character)
                        : conversation.relationships());
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new RoleplayException("Message content cannot be empty", "INVALID_REQUEST", 400);
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new RoleplayException("Message content exceeds maximum length of " + MAX_MESSAGE_LENGTH, "INVALID_REQUEST", 400);
        }
    }
}
