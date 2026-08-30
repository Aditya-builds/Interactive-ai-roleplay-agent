package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Story(
        String id,
        String title,
        String worldId,
        String premise,
        String openingNarrative,
        List<String> startingCharacters,
        String startingLocation,
        List<String> storyRules,
        List<Relationship> startingRelationships) {

    public Story {
        startingCharacters = startingCharacters != null ? List.copyOf(startingCharacters) : List.of();
        storyRules = storyRules != null ? List.copyOf(storyRules) : List.of();
        startingRelationships = startingRelationships != null ? List.copyOf(startingRelationships) : List.of();
    }
}
