package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ActorProfile(
        String description,
        List<String> personality,
        String background,
        List<String> values,
        String speakingStyle) {

    public ActorProfile {
        personality = personality != null ? List.copyOf(personality) : List.of();
        values = values != null ? List.copyOf(values) : List.of();
    }
}
