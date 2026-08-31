package com.aditya.roleplay.model.visual.reference;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReferenceAction {
    STANDING,
    WALKING,
    REACHING,
    COMBAT,
    CASTING,
    RESTING,
    LISTENING,
    EXPLORING,
    UNKNOWN;

    @JsonCreator
    public static ReferenceAction fromValue(String value) {
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
