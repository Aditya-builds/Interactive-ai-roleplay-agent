package com.aditya.roleplay.model.visual.reference;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReferenceExpression {
    NEUTRAL,
    HAPPY,
    SERIOUS,
    ANGRY,
    SAD,
    THOUGHTFUL,
    SURPRISED,
    DETERMINED,
    SERENE,
    PLAYFUL,
    UNKNOWN;

    @JsonCreator
    public static ReferenceExpression fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
