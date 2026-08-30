package com.aditya.roleplay.service;

import com.aditya.roleplay.model.CharacterPresence;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Scene;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class StoryStateService {

    public Scene createInitialScene(RoleplayCharacter character) {
        CharacterPresence presence = character.presence();
        if (presence != null) {
            return new Scene(
                    presence.defaultLocation(),
                    presence.defaultTime(),
                    List.of(character.id(), "user"),
                    presence.description(),
                    null);
        }

        return new Scene(
                "unknown",
                "unknown",
                List.of(character.id(), "user"),
                character.name() + " is present.",
                null);
    }

    public Scene applyPostTurnUpdates(Scene scene, String userMessage) {
        // V1: scene stays static unless manually edited in JSON
        return scene;
    }
}
