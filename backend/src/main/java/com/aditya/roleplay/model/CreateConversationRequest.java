package com.aditya.roleplay.model;

import java.util.List;

public record CreateConversationRequest(
        String characterId,
        String playerPersonaId,
        String storyId,
        String focalCharacterId,
        List<String> activeCharacterIds) {
}
