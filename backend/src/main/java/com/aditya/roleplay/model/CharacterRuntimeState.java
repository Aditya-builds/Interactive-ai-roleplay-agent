package com.aditya.roleplay.model;

public record CharacterRuntimeState(
        String characterId,
        CharacterHealth health,
        String location,
        String status,
        String emotion) {
}
