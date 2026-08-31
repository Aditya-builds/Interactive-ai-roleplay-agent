package com.aditya.roleplay.model.visual.reference;

import java.util.List;

public record ReferenceSelectionResult(
        List<CharacterReferenceImage> selectedImages,
        List<String> filesystemPaths,
        String selectionSummary) {

    public ReferenceSelectionResult {
        selectedImages = selectedImages != null ? List.copyOf(selectedImages) : List.of();
        filesystemPaths = filesystemPaths != null ? List.copyOf(filesystemPaths) : List.of();
    }

    public List<String> selectedReferenceIds() {
        return selectedImages.stream().map(CharacterReferenceImage::id).toList();
    }
}
