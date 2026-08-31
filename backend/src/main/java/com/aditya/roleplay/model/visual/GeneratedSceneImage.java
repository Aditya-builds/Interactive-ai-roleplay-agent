package com.aditya.roleplay.model.visual;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedSceneImage(
        String id,
        String conversationId,
        List<String> characterIds,
        String sourceMessageId,
        String prompt,
        String negativePrompt,
        String provider,
        String model,
        String imageUrl,
        Instant createdAt) {

    public GeneratedSceneImage {
        characterIds = characterIds != null ? List.copyOf(characterIds) : List.of();
    }
}
