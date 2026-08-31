package com.aditya.roleplay.model.visual.reference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterReferenceImageSummary(
        String id,
        String imageType,
        String pose,
        String cameraAngle,
        String expression,
        String clothing,
        String action,
        String framing,
        String notes,
        int priority,
        boolean identityAnchor,
        boolean canonical,
        String imageUrl) {
}
