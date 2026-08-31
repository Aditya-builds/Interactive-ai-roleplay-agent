package com.aditya.roleplay.model.visual.reference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterReferenceLibrarySummary(
        String characterId,
        String canonicalReferenceId,
        int imageCount,
        List<CharacterReferenceImageSummary> images) {

    public CharacterReferenceLibrarySummary {
        images = images != null ? List.copyOf(images) : List.of();
    }
}
