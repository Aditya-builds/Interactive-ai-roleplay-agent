package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerPersona(
        String id,
        String name,
        String worldId,
        String imageUrl,
        ActorProfile profile,
        Appearance appearance,
        List<String> abilities,
        List<String> goals) {

    public PlayerPersona {
        profile = profile != null ? profile : new ActorProfile(null, List.of(), null, List.of(), null);
        appearance = appearance != null ? appearance : new Appearance(null, null, null, null, null);
        abilities = abilities != null ? List.copyOf(abilities) : List.of();
        goals = goals != null ? List.copyOf(goals) : List.of();
    }

    public String type() {
        return ActorType.PLAYER_PERSONA.name();
    }
}
