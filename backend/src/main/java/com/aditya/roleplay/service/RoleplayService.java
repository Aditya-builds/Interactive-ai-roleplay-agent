package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.LlmException;
import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.llm.LlmClient;
import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.llm.LlmResponse;
import com.aditya.roleplay.llm.LlmTurnResult;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.SendMessageResponse;
import com.aditya.roleplay.model.World;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
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
    PromptService promptService;

    @Inject
    StateChangeProcessor stateChangeProcessor;

    @Inject
    StoryStateService storyStateService;

    @Inject
    RelationshipService relationshipService;

    @Inject
    LlmClient llmClient;

    public SendMessageResponse processTurn(String conversationId, String content) {
        validateContent(content);
        String trimmed = content.trim();

        Conversation conversation = conversationService.getConversation(conversationId);
        RoleplayCharacter character = characterService.requireCharacter(conversation.characterId());
        World world = characterService.requireWorld(conversation.worldId());
        conversation = ensureRuntimeFields(conversation, character);

        Set<String> allowedRelationshipTargets = characterService.allowedRelationshipTargets(
                conversation.worldId(),
                conversation.characterId());

        LlmRequest llmRequest = promptService.build(character, world, conversation, trimmed, allowedRelationshipTargets);
        LlmResponse llmResponse = llmClient.complete(llmRequest);

        if (!llmResponse.structuredParseSuccess() || llmResponse.turnResult() == null) {
            throw new LlmException("LLM returned invalid structured output. Turn was not saved.");
        }

        LlmTurnResult turnResult = llmResponse.turnResult();

        Message userMessage = new Message(
                UUID.randomUUID().toString(),
                Role.USER,
                trimmed,
                Instant.now());

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
                allowedRelationshipTargets);

        Message assistantMessage = new Message(
                UUID.randomUUID().toString(),
                Role.ASSISTANT,
                turnResult.response().trim(),
                Instant.now());

        conversation = conversation
                .appendMessage(userMessage)
                .appendMessage(assistantMessage)
                .withCharacterState(updatedState.characterState())
                .withScene(updatedState.scene())
                .withRelationships(updatedState.relationships())
                .withEvents(updatedState.events())
                .withMemories(updatedState.memories())
                .withUpdatedAt(Instant.now());

        conversationService.save(conversation);

        return new SendMessageResponse(
                assistantMessage,
                conversation.id(),
                updatedState.scene(),
                updatedState.characterState(),
                updatedState.relationships());
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
