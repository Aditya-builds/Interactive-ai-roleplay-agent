package com.aditya.roleplay.model;

import java.time.Instant;

public record StoryMemoryEntry(
        String id,
        String content,
        Instant createdAt,
        String source,
        Double importance) {
}
