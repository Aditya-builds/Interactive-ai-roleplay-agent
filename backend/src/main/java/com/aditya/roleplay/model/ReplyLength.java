package com.aditya.roleplay.model;

public enum ReplyLength {
    SHORT,
    NORMAL,
    LONG;

    public static ReplyLength fromString(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return ReplyLength.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
