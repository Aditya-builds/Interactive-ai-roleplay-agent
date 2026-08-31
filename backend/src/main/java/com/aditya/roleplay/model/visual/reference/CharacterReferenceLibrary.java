package com.aditya.roleplay.model.visual.reference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterReferenceLibrary(
        String characterId,
        String canonicalReferenceId,
        String directory,
        List<CharacterReferenceImage> images) {

    public CharacterReferenceLibrary {
        if (characterId == null) {
            characterId = "";
        }
        if (canonicalReferenceId == null) {
            canonicalReferenceId = "";
        }
        if (directory == null) {
            directory = "";
        }
        images = images != null ? List.copyOf(images) : List.of();
    }

    public Optional<CharacterReferenceImage> findById(String referenceId) {
        return images.stream()
                .filter(image -> image.id().equals(referenceId))
                .findFirst();
    }

    public Optional<CharacterReferenceImage> canonicalReference() {
        if (!canonicalReferenceId.isBlank()) {
            Optional<CharacterReferenceImage> canonical = findById(canonicalReferenceId);
            if (canonical.isPresent()) {
                return canonical;
            }
        }
        return images.stream()
                .filter(image -> image.imageType() == ReferenceImageType.CANONICAL || image.identityAnchor())
                .findFirst();
    }
}
