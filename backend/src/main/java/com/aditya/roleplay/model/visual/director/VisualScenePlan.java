package com.aditya.roleplay.model.visual.director;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VisualScenePlan(
        boolean shouldGenerate,
        String momentType,
        String reasoningSummary,
        List<VisualPlanCharacter> characters,
        SceneCompositionPlan scene,
        InteractionPlan interaction,
        String prompt,
        String negativePrompt,
        long graphExecutionMs) {

    public VisualScenePlan {
        characters = characters != null ? List.copyOf(characters) : List.of();
    }
}
