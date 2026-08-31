package com.aditya.roleplay.model.visual;

public record VisualCharacterScenePresence(
        String characterId,
        String name,
        String pose,
        String expression,
        String action,
        String sceneClothing) {
}
