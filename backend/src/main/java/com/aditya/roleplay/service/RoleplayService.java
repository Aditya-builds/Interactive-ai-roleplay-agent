package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.llm.LlmClient;
import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.llm.LlmResponse;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Scene;
import com.aditya.roleplay.model.SendMessageResponse;
import com.aditya.roleplay.model.World;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
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
    StoryStateService storyStateService;

    @Inject
    RelationshipService relationshipService;

    @Inject
    LlmClient llmClient;

    public SendMessageResponse processTurn(String conversationId, String content) {
        validateContent(content);

        Conversation conversation = conversationService.getConversation(conversationId);
        RoleplayCharacter character = characterService.requireCharacter(conversation.characterId());
        World world = characterService.requireWorld(conversation.worldId());

        Message userMessage = new Message(
                UUID.randomUUID().toString(),
                Role.USER,
                content.trim(),
                Instant.now());

        conversation = conversation.appendMessage(userMessage);

        LlmRequest llmRequest = promptService.build(character, world, conversation, content.trim());
        LlmResponse llmResponse = llmClient.complete(llmRequest);

        Message assistantMessage = new Message(
                UUID.randomUUID().toString(),
                Role.ASSISTANT,
                llmResponse.content().trim(),
                Instant.now());

        conversation = conversation.appendMessage(assistantMessage);

        Scene updatedScene = storyStateService.applyPostTurnUpdates(conversation.scene(), content);
        Relationship updatedRelationship = relationshipService.applyPostTurnUpdates(conversation.relationship(), content);

        conversation = conversation
                .withScene(updatedScene)
                .withRelationship(updatedRelationship)
                .withUpdatedAt(Instant.now());

        conversationService.save(conversation);

        return new SendMessageResponse(
                assistantMessage,
                conversation.id(),
                updatedScene,
                updatedRelationship);
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
