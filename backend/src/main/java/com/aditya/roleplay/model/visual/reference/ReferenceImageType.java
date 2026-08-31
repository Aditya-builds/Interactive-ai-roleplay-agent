package com.aditya.roleplay.model.visual.reference;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReferenceImageType {
    CANONICAL,
    FACE,
    FULL_BODY,
    POSE,
    EXPRESSION,
    ACTION,
    OUTFIT,
    ANGLE,
    UNKNOWN;

    @JsonCreator
    public static ReferenceImageType fromValue(String value) {
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
