package com.aditya.roleplay.model.visual.director;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SceneCompositionPlan(
        String location,
        String locationDescription,
        String time,
        String environment,
        String lighting,
        String atmosphere,
        String camera,
        String framing,
        String composition,
        String background) {
}
