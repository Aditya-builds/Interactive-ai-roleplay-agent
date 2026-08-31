package com.aditya.roleplay.model.visual.director;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SceneContext(
        String location,
        String locationDescription,
        String time,
        String currentSituation,
        String currentConflict,
        List<String> charactersPresent) {

    public SceneContext {
        charactersPresent = charactersPresent != null ? List.copyOf(charactersPresent) : List.of();
    }
}
