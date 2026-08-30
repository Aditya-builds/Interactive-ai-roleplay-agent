package com.aditya.roleplay.service;

import com.aditya.roleplay.model.CharacterHealth;
import com.aditya.roleplay.model.CharacterPresence;
import com.aditya.roleplay.model.CharacterRuntimeState;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Scene;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class StoryStateService {

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    public CharacterRuntimeState createInitialCharacterState(RoleplayCharacter character) {
        CharacterHealth health = character.health() != null
                ? character.health()
                : new CharacterHealth(100, 100);
        return new CharacterRuntimeState(character.id(), health, null, null);
    }

    public Scene applySceneChange(Scene scene, StateChange change) {
        return switch (change.field()) {
            case "location" -> new Scene(
                    change.value(),
                    scene.time(),
                    scene.charactersPresent(),
                    scene.currentSituation(),
                    scene.currentConflict());
            case "time" -> new Scene(
                    scene.location(),
                    change.value(),
                    scene.charactersPresent(),
                    scene.currentSituation(),
                    scene.currentConflict());
            case "currentSituation" -> new Scene(
                    scene.location(),
                    scene.time(),
                    scene.charactersPresent(),
                    change.value(),
                    scene.currentConflict());
            case "currentConflict" -> new Scene(
                    scene.location(),
                    scene.time(),
                    scene.charactersPresent(),
                    scene.currentSituation(),
                    "null".equalsIgnoreCase(change.value()) ? null : change.value());
            case "charactersPresent" -> new Scene(
                    scene.location(),
                    scene.time(),
                    parseCharactersPresent(change.value()),
                    scene.currentSituation(),
                    scene.currentConflict());
            default -> scene;
        };
    }

    public CharacterRuntimeState applyHealthChange(CharacterRuntimeState state, StateChange change) {
        CharacterHealth health = state.health();
        int max = health.max();
        int current = applyNumericValue(health.current(), change, max);
        return new CharacterRuntimeState(
                state.characterId(),
                new CharacterHealth(current, max),
                state.status(),
                state.emotion());
    }

    public CharacterRuntimeState applyStatusChange(CharacterRuntimeState state, StateChange change) {
        String status = "null".equalsIgnoreCase(change.value()) ? null : change.value();
        return new CharacterRuntimeState(
                state.characterId(),
                state.health(),
                status,
                state.emotion());
    }

    public CharacterRuntimeState applyEmotionChange(CharacterRuntimeState state, StateChange change) {
        String emotion = "null".equalsIgnoreCase(change.value()) ? null : change.value();
        return new CharacterRuntimeState(
                state.characterId(),
                state.health(),
                state.status(),
                emotion);
    }

    private List<String> parseCharactersPresent(String value) {
        try {
            List<String> parsed = objectMapper.readValue(value.trim(), new TypeReference<>() {});
            if (parsed.isEmpty()) {
                throw new IllegalArgumentException("charactersPresent cannot be empty");
            }
            return List.copyOf(parsed);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid charactersPresent JSON array: " + value, e);
        }
    }

    private int applyNumericValue(int current, StateChange change, int max) {
        int parsed = Integer.parseInt(change.value().trim());
        int result = switch (change.operation()) {
            case INCREASE -> current + parsed;
            case DECREASE -> current - parsed;
            case SET -> parsed;
            default -> current;
        };
        return Math.max(0, Math.min(max, result));
    }
}
