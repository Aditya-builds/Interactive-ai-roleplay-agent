package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterRuntimeState(
        String characterId,
        CharacterHealth health,
        String location,
        String status,
        String emotion) {
}
