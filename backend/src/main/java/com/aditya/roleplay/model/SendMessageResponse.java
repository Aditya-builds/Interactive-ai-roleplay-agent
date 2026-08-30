package com.aditya.roleplay.model;

import java.util.List;

public record SendMessageResponse(
        Message message,
        String conversationId,
        Scene scene,
        CharacterRuntimeState characterState,
        List<Relationship> relationships) {
}
