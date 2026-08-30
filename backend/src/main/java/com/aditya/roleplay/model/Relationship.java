package com.aditya.roleplay.model;

public record Relationship(
        String characterId,
        int trust,
        int respect,
        int affection,
        int familiarity,
        int suspicion) {

    public Relationship clamped() {
        return new Relationship(
                characterId,
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
