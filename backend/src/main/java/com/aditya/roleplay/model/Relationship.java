package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Relationship(
        String sourceId,
        String targetId,
        int trust,
        int respect,
        int affection,
        int familiarity,
        int suspicion) {

    /** Legacy constructor — sourceId resolved at runtime to the focal AI character. */
    public Relationship(String targetId, int trust, int respect, int affection, int familiarity, int suspicion) {
        this(null, targetId, trust, respect, affection, familiarity, suspicion);
    }

    public Relationship withSourceId(String resolvedSourceId) {
        if (resolvedSourceId == null || resolvedSourceId.equals(sourceId)) {
            return this;
        }
        return new Relationship(resolvedSourceId, targetId, trust, respect, affection, familiarity, suspicion);
    }

    public Relationship clamped() {
        return new Relationship(
                sourceId,
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
