package com.aditya.roleplay.visual;

public record ImageGenerationResponse(
        byte[] imageBytes,
        String mimeType,
        String provider,
        String model) {
}
