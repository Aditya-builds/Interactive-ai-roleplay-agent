package com.aditya.roleplay.model;

import java.time.Instant;

public record Message(
        String id,
        Role role,
        String content,
        Instant timestamp) {
}
