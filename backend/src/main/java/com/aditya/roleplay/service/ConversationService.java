package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.ConversationSummary;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.storage.JsonStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ConversationService {

    @Inject
    JsonStorageService storage;

    @Inject
    CharacterService characterService;

    @Inject
    StoryStateService storyStateService;

    @Inject
    RelationshipService relationshipService;

    public Conversation createConversation(String characterId) {
        RoleplayCharacter character = characterService.requireCharacter(characterId);
        characterService.requireWorld(character.worldId());

        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();

        Conversation conversation = new Conversation(
                id,
                character.id(),
                character.worldId(),
                now,
                now,
                storyStateService.createInitialCharacterState(character),
                storyStateService.createInitialScene(character),
                relationshipService.createInitialRelationships(character),
                List.of(),
                characterService.seedMemoriesForCharacter(character, now),
                List.of());

        return storage.saveConversation(conversation);
    }

    public Conversation getConversation(String id) {
        Conversation conversation = storage.loadConversation(id)
                .orElseThrow(() -> new RoleplayException("Conversation not found: " + id, "CONVERSATION_NOT_FOUND", 404));
        return migrateIfNeeded(conversation);
    }

    public Conversation save(Conversation conversation) {
        return storage.saveConversation(conversation);
    }

    public List<ConversationSummary> listConversations() {
        return storage.listConversations();
    }

    public void deleteConversation(String id) {
        storage.loadConversation(id)
                .orElseThrow(() -> new RoleplayException("Conversation not found: " + id, "CONVERSATION_NOT_FOUND", 404));
        storage.deleteConversation(id);
    }

    private Conversation migrateIfNeeded(Conversation conversation) {
        boolean needsSave = false;
        Conversation updated = conversation;

        if (conversation.characterState() == null) {
            RoleplayCharacter character = characterService.requireCharacter(conversation.characterId());
            updated = updated.withCharacterState(storyStateService.createInitialCharacterState(character));
            needsSave = true;
        }
        if (conversation.events() == null) {
            updated = updated.withEvents(List.of());
            needsSave = true;
        }
        if (conversation.relationships() == null || conversation.relationships().isEmpty()) {
            RoleplayCharacter character = characterService.requireCharacter(conversation.characterId());
            updated = updated.withRelationships(relationshipService.createInitialRelationships(character));
            needsSave = true;
        }

        if (needsSave) {
            return storage.saveConversation(updated);
        }
        return conversation;
    }
}
