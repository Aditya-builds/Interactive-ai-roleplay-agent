package com.aditya.roleplay.model;

import java.util.List;

public record CreatePersonaRequest(
        String id,
        String name,
        String worldId,
        String description,
        List<String> personality,
        String background,
        String speakingStyle,
        String imageUrl) {
}
