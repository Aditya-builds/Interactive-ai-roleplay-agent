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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class StoryStateService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Scene createInitialScene(RoleplayCharacter character) {
        CharacterPresence presence = character.presence();
        if (presence != null) {
            String location = presence.defaultLocation();
            return new Scene(
                    location,
                    location,
                    presence.defaultTime(),
                    List.of(character.id(), "user"),
                    presence.description(),
                    null);
        }

        return new Scene(
                "unknown",
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
        String location = character.presence() != null
                ? character.presence().defaultLocation()
                : "unknown";
        return new CharacterRuntimeState(character.id(), health, location, null, null);
    }

    /** Ensures NPC runtime location matches scene.location (NPC scene anchor). */
    public CharacterRuntimeState reconcileCharacterLocation(CharacterRuntimeState state, Scene scene) {
        if (state == null || scene == null) {
            return state;
        }
        if (scene.location().equals(state.location())) {
            return state;
        }
        return new CharacterRuntimeState(
                state.characterId(),
                state.health(),
                scene.location(),
                state.status(),
                state.emotion());
    }

    public Scene applySceneChange(Scene scene, StateChange change) {
        return switch (change.field()) {
            case "location" -> applyNpcLocationChange(scene, change.value());
            case "userLocation" -> applyUserLocationChange(scene, change.value());
            case "time" -> copyScene(scene, scene.location(), scene.userLocation(), change.value(),
                    scene.charactersPresent(), scene.currentSituation(), scene.currentConflict());
            case "currentSituation" -> copyScene(scene, scene.location(), scene.userLocation(), scene.time(),
                    scene.charactersPresent(),
                    "null".equalsIgnoreCase(change.value()) ? null : change.value(),
                    scene.currentConflict());
            case "currentConflict" -> copyScene(scene, scene.location(), scene.userLocation(), scene.time(),
                    scene.charactersPresent(), scene.currentSituation(),
                    "null".equalsIgnoreCase(change.value()) ? null : change.value());
            case "charactersPresent" -> applyCharactersPresentChange(scene, parseCharactersPresent(change.value()));
            case "addCharacter" -> applyAddCharacter(scene, change.value());
            case "removeCharacter" -> applyRemoveCharacter(scene, change.value());
            default -> scene;
        };
    }

    public Scene applyNpcLocationChange(Scene scene, String location) {
        String userLocation = scene.charactersPresent().contains("user") ? location : scene.userLocation();
        return copyScene(scene, location, userLocation, scene.time(), scene.charactersPresent(), null, scene.currentConflict());
    }

    public Scene applyUserLocationChange(Scene scene, String location) {
        List<String> present = new ArrayList<>(scene.charactersPresent());
        if (!location.equals(scene.location())) {
            present.removeIf("user"::equals);
        } else if (!present.contains("user")) {
            present.add("user");
        }
        if (present.isEmpty()) {
            throw new IllegalArgumentException("charactersPresent cannot be empty");
        }
        return copyScene(scene, scene.location(), location, scene.time(), dedupePreservingOrder(present), null,
                scene.currentConflict());
    }

    public CharacterRuntimeState applyHealthChange(CharacterRuntimeState state, StateChange change) {
        CharacterHealth health = state.health();
        int max = health.max();
        int current = applyNumericValue(health.current(), change, max);
        return new CharacterRuntimeState(
                state.characterId(),
                new CharacterHealth(current, max),
                state.location(),
                state.status(),
                state.emotion());
    }

    public CharacterRuntimeState applyLocationChange(CharacterRuntimeState state, StateChange change) {
        return new CharacterRuntimeState(
                state.characterId(),
                state.health(),
                change.value(),
                state.status(),
                state.emotion());
    }

    public CharacterRuntimeState applyStatusChange(CharacterRuntimeState state, StateChange change) {
        String status = "null".equalsIgnoreCase(change.value()) ? null : change.value();
        return new CharacterRuntimeState(
                state.characterId(),
                state.health(),
                state.location(),
                status,
                state.emotion());
    }

    public CharacterRuntimeState applyEmotionChange(CharacterRuntimeState state, StateChange change) {
        String emotion = "null".equalsIgnoreCase(change.value()) ? null : change.value();
        return new CharacterRuntimeState(
                state.characterId(),
                state.health(),
                state.location(),
                state.status(),
                emotion);
    }

    private Scene applyCharactersPresentChange(Scene scene, List<String> ids) {
        return copyScene(scene, scene.location(), scene.userLocation(), scene.time(), ids, null, scene.currentConflict());
    }

    private Scene applyAddCharacter(Scene scene, String characterId) {
        List<String> updated = new ArrayList<>(scene.charactersPresent());
        if (!updated.contains(characterId)) {
            updated.add(characterId);
        }
        if (scene.userCoLocated() && !updated.contains("user")) {
            updated.add("user");
        }
        return copyScene(scene, scene.location(), scene.userLocation(), scene.time(),
                dedupePreservingOrder(updated), scene.currentSituation(), scene.currentConflict());
    }

    private Scene applyRemoveCharacter(Scene scene, String characterId) {
        List<String> updated = new ArrayList<>(scene.charactersPresent());
        updated.removeIf(id -> id.equals(characterId));
        if (updated.isEmpty()) {
            throw new IllegalArgumentException("charactersPresent cannot be empty");
        }
        String situation = "user".equals(characterId) ? null : scene.currentSituation();
        return copyScene(scene, scene.location(), scene.userLocation(), scene.time(),
                dedupePreservingOrder(updated), situation, scene.currentConflict());
    }

    private Scene copyScene(
            Scene scene,
            String location,
            String userLocation,
            String time,
            List<String> charactersPresent,
            String currentSituation,
            String currentConflict) {
        return new Scene(location, userLocation, time, charactersPresent, currentSituation, currentConflict);
    }

    private List<String> parseCharactersPresent(String value) {
        try {
            List<String> parsed = objectMapper.readValue(value.trim(), new TypeReference<>() {});
            if (parsed.isEmpty()) {
                throw new IllegalArgumentException("charactersPresent cannot be empty");
            }
            return dedupePreservingOrder(parsed);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid charactersPresent JSON array: " + value, e);
        }
    }

    private List<String> dedupePreservingOrder(List<String> ids) {
        Set<String> seen = new LinkedHashSet<>();
        for (String id : ids) {
            seen.add(id);
        }
        return List.copyOf(seen);
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
