package com.aditya.roleplay.model.visual.director;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InteractionPlan(
        String focus,
        String distance,
        String bodyLanguage,
        String emotionalTension) {
}
