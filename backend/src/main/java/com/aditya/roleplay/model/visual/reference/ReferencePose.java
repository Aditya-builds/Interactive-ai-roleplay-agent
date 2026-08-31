package com.aditya.roleplay.model.visual.reference;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReferencePose {
    STANDING,
    SITTING,
    WALKING,
    REACHING,
    COMBAT,
    CASTING,
    RESTING,
    KNEELING,
    LEANING,
    UNKNOWN;

    @JsonCreator
    public static ReferencePose fromValue(String value) {
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
