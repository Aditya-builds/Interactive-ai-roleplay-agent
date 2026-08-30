package com.aditya.roleplay.model;

public record SendMessageResponse(
        Message message,
        String conversationId,
        Scene scene,
        Relationship relationship) {
}
