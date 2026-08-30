package com.aditya.roleplay.model;

import java.util.List;

public record RoleplayCharacter(
        String id,
        String worldId,
        String name,
        String imageUrl,
        CharacterHealth health,
        List<String> personality,
        String background,
        String speakingStyle,
        List<String> values,
        CharacterPresence presence) {
}
