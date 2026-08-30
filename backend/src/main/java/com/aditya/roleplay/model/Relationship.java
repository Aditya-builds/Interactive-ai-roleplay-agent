package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Relationship(
        String targetId,
        int trust,
        int respect,
        int affection,
        int familiarity,
        int suspicion) {

    public Relationship clamped() {
        return new Relationship(
                targetId,
                clamp(trust),
                clamp(respect),
                clamp(affection),
                clamp(familiarity),
                clamp(suspicion));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
