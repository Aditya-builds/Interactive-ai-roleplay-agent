package com.aditya.roleplay.model.visual.director;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VisualPlanCharacter(
        String characterId,
        String name,
        String referenceImage,
        String pose,
        String expression,
        String action,
        String position,
        String sceneClothing) {
}
