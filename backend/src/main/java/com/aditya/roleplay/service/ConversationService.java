package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.ConversationSummary;
import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Scene;
import com.aditya.roleplay.model.StoryMemoryEntry;
import com.aditya.roleplay.storage.JsonStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
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
                storyStateService.createInitialScene(character),
                relationshipService.createInitialRelationship(character.id()),
                defaultMemories(now),
                List.of());

        return storage.saveConversation(conversation);
    }

    public Conversation getConversation(String id) {
        return storage.loadConversation(id)
                .orElseThrow(() -> new RoleplayException("Conversation not found: " + id, "CONVERSATION_NOT_FOUND", 404));
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

    private List<StoryMemoryEntry> defaultMemories(Instant now) {
        List<StoryMemoryEntry> memories = new ArrayList<>();
        memories.add(new StoryMemoryEntry(
                UUID.randomUUID().toString(),
                "The user protected Aurora during the forest mission.",
                now.minusSeconds(86400),
                "manual"));
        return memories;
    }
}
