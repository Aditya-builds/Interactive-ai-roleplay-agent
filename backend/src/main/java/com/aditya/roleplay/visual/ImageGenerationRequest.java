package com.aditya.roleplay.visual;

import java.util.List;

public record ImageGenerationRequest(
        String prompt,
        String negativePrompt,
        List<String> referenceImagePaths,
        String aspectRatio,
        int width,
        int height,
        Long seed,
        String model) {

    public ImageGenerationRequest {
        referenceImagePaths = referenceImagePaths != null ? List.copyOf(referenceImagePaths) : List.of();
    }
}
