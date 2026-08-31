package com.aditya.roleplay.model.visual.reference;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReferenceFraming {
    CLOSE_UP,
    MEDIUM,
    FULL_BODY,
    UNKNOWN;

    @JsonCreator
    public static ReferenceFraming fromValue(String value) {
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
