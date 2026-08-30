package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoleplayCharacter(
        String id,
        String worldId,
        String name,
        String imageUrl,
        CharacterHealth health,
        List<String> personality,
        String background,
        String speakingStyle,
        List<String> values,
        CharacterPresence presence,
        List<Relationship> defaultRelationships,
        List<SeedMemory> seedMemories,
        String openingMessage,
        List<String> abilities,
        List<String> goals) {

    public RoleplayCharacter {
        personality = personality != null ? List.copyOf(personality) : List.of();
        values = values != null ? List.copyOf(values) : List.of();
        defaultRelationships = defaultRelationships != null ? List.copyOf(defaultRelationships) : List.of();
        seedMemories = seedMemories != null ? List.copyOf(seedMemories) : List.of();
        abilities = abilities != null ? List.copyOf(abilities) : List.of();
        goals = goals != null ? List.copyOf(goals) : List.of();
    }

    /** Backward-compatible constructor without abilities/goals. */
    public RoleplayCharacter(
            String id,
            String worldId,
            String name,
            String imageUrl,
            CharacterHealth health,
            List<String> personality,
            String background,
            String speakingStyle,
            List<String> values,
            CharacterPresence presence,
            List<Relationship> defaultRelationships,
            List<SeedMemory> seedMemories,
            String openingMessage) {
        this(id, worldId, name, imageUrl, health, personality, background, speakingStyle, values,
                presence, defaultRelationships, seedMemories, openingMessage, List.of(), List.of());
    }

    public String type() {
        return ActorType.AI_CHARACTER.name();
    }
}
