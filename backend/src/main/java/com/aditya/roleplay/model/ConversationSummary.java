package com.aditya.roleplay.model;

import java.time.Instant;

public record ConversationSummary(
        String id,
        String characterId,
        String characterName,
        String preview,
        int messageCount,
        Instant updatedAt) {
}
