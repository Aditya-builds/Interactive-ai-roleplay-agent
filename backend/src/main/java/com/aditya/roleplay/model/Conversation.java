package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Conversation(
        String id,
        String characterId,
        String worldId,
        Instant createdAt,
        Instant updatedAt,
        CharacterRuntimeState characterState,
        Scene scene,
        List<Relationship> relationships,
        List<StoryEvent> events,
        List<StoryMemoryEntry> memories,
        List<Message> messages,
        String playerPersonaId,
        String storyId,
        List<String> activeCharacterIds,
        CharacterRuntimeState playerPersonaState) {

    public Conversation {
        relationships = relationships != null ? List.copyOf(relationships) : List.of();
        events = events != null ? List.copyOf(events) : List.of();
        memories = memories != null ? List.copyOf(memories) : List.of();
        messages = messages != null ? List.copyOf(messages) : List.of();
        activeCharacterIds = activeCharacterIds != null ? List.copyOf(activeCharacterIds) : List.of();
    }

    /** Backward-compatible constructor for legacy conversations. */
    public Conversation(
            String id,
            String characterId,
            String worldId,
            Instant createdAt,
            Instant updatedAt,
            CharacterRuntimeState characterState,
            Scene scene,
            List<Relationship> relationships,
            List<StoryEvent> events,
            List<StoryMemoryEntry> memories,
            List<Message> messages) {
        this(id, characterId, worldId, createdAt, updatedAt, characterState, scene, relationships,
                events, memories, messages, null, null, List.of(characterId), null);
    }

    public String focalCharacterId() {
        return characterId;
    }

    public String resolvedPlayerPersonaId() {
        return playerPersonaId != null && !playerPersonaId.isBlank() ? playerPersonaId : "user";
    }

    public List<String> resolvedActiveCharacterIds() {
        if (!activeCharacterIds.isEmpty()) {
            return activeCharacterIds;
        }
        return characterId != null ? List.of(characterId) : List.of();
    }

    public Relationship relationshipFrom(String sourceId, String targetId) {
        return relationships.stream()
                .filter(r -> matchesSource(r, sourceId) && targetId.equals(r.targetId()))
                .findFirst()
                .orElse(new Relationship(sourceId, targetId, 40, 50, 10, 20, 5));
    }

    public Relationship userRelationship() {
        return relationshipFrom(characterId, "user");
    }

    public Relationship personaRelationship(String personaId) {
        return relationshipFrom(characterId, personaId);
    }

    private static boolean matchesSource(Relationship relationship, String sourceId) {
        return relationship.sourceId() == null || sourceId.equals(relationship.sourceId());
    }

    public Conversation withMessages(List<Message> newMessages) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene,
                relationships, events, memories, newMessages, playerPersonaId, storyId, activeCharacterIds, playerPersonaState);
    }

    public Conversation withUpdatedAt(Instant newUpdatedAt) {
        return new Conversation(id, characterId, worldId, createdAt, newUpdatedAt, characterState, scene,
                relationships, events, memories, messages, playerPersonaId, storyId, activeCharacterIds, playerPersonaState);
    }

    public Conversation withCharacterState(CharacterRuntimeState newCharacterState) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, newCharacterState, scene,
                relationships, events, memories, messages, playerPersonaId, storyId, activeCharacterIds, playerPersonaState);
    }

    public Conversation withScene(Scene newScene) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, newScene,
                relationships, events, memories, messages, playerPersonaId, storyId, activeCharacterIds, playerPersonaState);
    }

    public Conversation withRelationships(List<Relationship> newRelationships) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene,
                newRelationships, events, memories, messages, playerPersonaId, storyId, activeCharacterIds, playerPersonaState);
    }

    public Conversation withEvents(List<StoryEvent> newEvents) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene,
                relationships, newEvents, memories, messages, playerPersonaId, storyId, activeCharacterIds, playerPersonaState);
    }

    public Conversation withMemories(List<StoryMemoryEntry> newMemories) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene,
                relationships, events, newMemories, messages, playerPersonaId, storyId, activeCharacterIds, playerPersonaState);
    }

    public Conversation withPlayerPersonaState(CharacterRuntimeState state) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene,
                relationships, events, memories, messages, playerPersonaId, storyId, activeCharacterIds, state);
    }

    public Conversation appendMessage(Message message) {
        List<Message> updated = new ArrayList<>(messages);
        updated.add(message);
        return withMessages(updated);
    }
}
