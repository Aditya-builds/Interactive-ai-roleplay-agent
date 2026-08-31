package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public record Message(
        String id,
        Role role,
        String content,
        Instant timestamp,
        String sceneImageId) {

    public Message(String id, Role role, String content, Instant timestamp) {
        this(id, role, content, timestamp, null);
    }

    @JsonIgnore
    public boolean isSceneImage() {
        return sceneImageId != null && !sceneImageId.isBlank();
    }
}
