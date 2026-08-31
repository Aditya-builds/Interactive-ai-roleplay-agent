package com.aditya.roleplay.model.visual.reference;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReferenceClothing {
    CANONICAL,
    ALTERNATE,
    UNKNOWN;

    @JsonCreator
    public static ReferenceClothing fromValue(String value) {
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
