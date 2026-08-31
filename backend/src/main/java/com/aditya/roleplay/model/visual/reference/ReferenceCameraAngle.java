package com.aditya.roleplay.model.visual.reference;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReferenceCameraAngle {
    FRONT,
    THREE_QUARTER,
    SIDE,
    BACK,
    LOW_ANGLE,
    HIGH_ANGLE,
    CLOSE_UP,
    MEDIUM,
    FULL_BODY,
    UNKNOWN;

    @JsonCreator
    public static ReferenceCameraAngle fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_');
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
