package com.aditya.roleplay.model;

import java.time.Instant;
import java.util.List;

public record StoryEvent(
        String id,
        String description,
        Instant createdAt,
        List<String> participants,
        Double importance) {

    public StoryEvent {
        participants = participants != null ? List.copyOf(participants) : List.of();
    }
}
