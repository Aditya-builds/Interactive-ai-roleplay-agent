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
        String model,
        List<String> selectedReferenceIds,
        String referenceSelectionSummary) {

    public ImageGenerationRequest {
        referenceImagePaths = referenceImagePaths != null ? List.copyOf(referenceImagePaths) : List.of();
        selectedReferenceIds = selectedReferenceIds != null ? List.copyOf(selectedReferenceIds) : List.of();
    }

    public ImageGenerationRequest(
            String prompt,
            String negativePrompt,
            List<String> referenceImagePaths,
            String aspectRatio,
            int width,
            int height,
            Long seed,
            String model) {
        this(prompt, negativePrompt, referenceImagePaths, aspectRatio, width, height, seed, model, List.of(), null);
    }
}
