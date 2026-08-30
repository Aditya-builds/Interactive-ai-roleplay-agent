package com.aditya.roleplay.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        List<Message> messages) {

    public Conversation {
        relationships = relationships != null ? List.copyOf(relationships) : List.of();
        events = events != null ? List.copyOf(events) : List.of();
        memories = memories != null ? List.copyOf(memories) : List.of();
        messages = messages != null ? List.copyOf(messages) : List.of();
    }

    public Relationship userRelationship() {
        return relationships.stream()
                .filter(r -> "user".equals(r.targetId()))
                .findFirst()
                .orElse(new Relationship("user", 40, 50, 10, 20, 5));
    }

    public Conversation withMessages(List<Message> newMessages) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene, relationships, events, memories, newMessages);
    }

    public Conversation withUpdatedAt(Instant newUpdatedAt) {
        return new Conversation(id, characterId, worldId, createdAt, newUpdatedAt, characterState, scene, relationships, events, memories, messages);
    }

    public Conversation withCharacterState(CharacterRuntimeState newCharacterState) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, newCharacterState, scene, relationships, events, memories, messages);
    }

    public Conversation withScene(Scene newScene) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, newScene, relationships, events, memories, messages);
    }

    public Conversation withRelationships(List<Relationship> newRelationships) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene, newRelationships, events, memories, messages);
    }

    public Conversation withEvents(List<StoryEvent> newEvents) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene, relationships, newEvents, memories, messages);
    }

    public Conversation withMemories(List<StoryMemoryEntry> newMemories) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene, relationships, events, newMemories, messages);
    }

    public Conversation appendMessage(Message message) {
        List<Message> updated = new ArrayList<>(messages);
        updated.add(message);
        return withMessages(updated);
    }
}
