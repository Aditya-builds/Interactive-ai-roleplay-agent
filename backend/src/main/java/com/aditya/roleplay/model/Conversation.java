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
        Scene scene,
        Relationship relationship,
        List<StoryMemoryEntry> memories,
        List<Message> messages) {

    public Conversation {
        memories = memories != null ? List.copyOf(memories) : List.of();
        messages = messages != null ? List.copyOf(messages) : List.of();
    }

    public Conversation withMessages(List<Message> newMessages) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, scene, relationship, memories, newMessages);
    }

    public Conversation withUpdatedAt(Instant newUpdatedAt) {
        return new Conversation(id, characterId, worldId, createdAt, newUpdatedAt, scene, relationship, memories, messages);
    }

    public Conversation withScene(Scene newScene) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, newScene, relationship, memories, messages);
    }

    public Conversation withRelationship(Relationship newRelationship) {
        return new Conversation(id, characterId, worldId, createdAt, updatedAt, scene, newRelationship, memories, messages);
    }

    public Conversation appendMessage(Message message) {
        List<Message> updated = new ArrayList<>(messages);
        updated.add(message);
        return withMessages(updated);
    }
}
