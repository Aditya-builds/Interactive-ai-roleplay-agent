package com.aditya.roleplay.model.visual;

import java.util.List;

public record VisualSceneState(
        String location,
        String locationDescription,
        String time,
        String situation,
        List<VisualCharacterScenePresence> characters,
        String camera,
        String lighting,
        String atmosphere,
        String recentMoment) {

    public VisualSceneState {
        characters = characters != null ? List.copyOf(characters) : List.of();
    }
}
