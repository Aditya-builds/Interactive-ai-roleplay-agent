package com.aditya.roleplay.model;

import java.util.List;

public record CreateStoryRequest(
        String id,
        String title,
        String worldId,
        String premise,
        String openingNarrative,
        List<String> startingCharacters,
        List<String> startingCharacterNames,
        String startingLocation,
        List<String> storyRules) {
}
