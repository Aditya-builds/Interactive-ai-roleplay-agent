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
        String openingMessage) {

    public RoleplayCharacter {
        defaultRelationships = defaultRelationships != null ? List.copyOf(defaultRelationships) : List.of();
        seedMemories = seedMemories != null ? List.copyOf(seedMemories) : List.of();
    }
}
