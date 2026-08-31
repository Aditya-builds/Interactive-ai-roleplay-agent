package com.aditya.roleplay.model;

import java.util.List;

public record CreateCharacterRequest(
        String id,
        String worldId,
        String name,
        String background,
        String speakingStyle,
        List<String> personality,
        String openingMessage,
        String imageUrl) {
}
