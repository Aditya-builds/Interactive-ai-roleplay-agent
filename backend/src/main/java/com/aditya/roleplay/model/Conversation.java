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
        Relationship relationship,
        List<StoryEvent> events,
        List<StoryMemoryEntry> memories,
        List<Message> messages) {

    public Conversation {
        events = events != null ? List.copyOf(events) : List.of();
        memories = memories != null ? List.copyOf(memories) : List.of();
        messages = messages != null ? List.copyOf(messages) : List.of();
    }

    public Conversation withMessages(List<Message> newMessages) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene, relationship, events, memories, newMessages);
    }

    public Conversation withUpdatedAt(Instant newUpdatedAt) {
        return new Conversation(id, characterId, worldId, createdAt, newUpdatedAt, characterState, scene, relationship, events, memories, messages);
    }

    public Conversation withCharacterState(CharacterRuntimeState newCharacterState) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, newCharacterState, scene, relationship, events, memories, messages);
    }

    public Conversation withScene(Scene newScene) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, newScene, relationship, events, memories, messages);
    }

    public Conversation withRelationship(Relationship newRelationship) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene, newRelationship, events, memories, messages);
    }

    public Conversation withEvents(List<StoryEvent> newEvents) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene, relationship, newEvents, memories, messages);
    }

    public Conversation withMemories(List<StoryMemoryEntry> newMemories) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, characterState, scene, relationship, events, newMemories, messages);
    }

    public Conversation appendMessage(Message message) {
        List<Message> updated = new ArrayList<>(messages);
        updated.add(message);
        return withMessages(updated);
    }
}
