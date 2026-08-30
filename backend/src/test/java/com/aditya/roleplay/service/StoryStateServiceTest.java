package com.aditya.roleplay.service;

import com.aditya.roleplay.model.CharacterHealth;
import com.aditya.roleplay.model.CharacterRuntimeState;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoryStateServiceTest {

    private final StoryStateService storyStateService = new StoryStateService();

    @Test
    void appliesHealthDamageWithoutGoingBelowZero() {
        CharacterRuntimeState state = new CharacterRuntimeState(
                "aurora",
                new CharacterHealth(30, 100),
                "guild_hall",
                null,
                null);

        CharacterRuntimeState updated = storyStateService.applyHealthChange(state, new StateChange(
                StateChangeType.HEALTH,
                "aurora",
                "current",
                StateChangeOperation.DECREASE,
                "50"));

        assertEquals(0, updated.health().current());
    }

    @Test
    void reconcilesCharacterLocationFromScene() {
        CharacterRuntimeState state = new CharacterRuntimeState(
                "aurora",
                new CharacterHealth(100, 100),
                "guild_hall",
                null,
                null);
        var scene = new com.aditya.roleplay.model.Scene(
                "forest", null, "evening", java.util.List.of("aurora", "user"), "In the woods.", null);

        CharacterRuntimeState synced = storyStateService.reconcileCharacterLocation(state, scene);

        assertEquals("forest", synced.location());
    }

    @Test
    void userLocationChangeRemovesUserFromPresenceAndClearsSituation() {
        var scene = new com.aditya.roleplay.model.Scene(
                "guild_hall", "guild_hall", "evening", java.util.List.of("aurora", "user"), "Sharing a meal.", null);

        var updated = storyStateService.applyUserLocationChange(scene, "forest");

        assertEquals("guild_hall", updated.location());
        assertEquals("forest", updated.userLocation());
        assertEquals(java.util.List.of("aurora"), updated.charactersPresent());
        assertNull(updated.currentSituation());
    }

    @Test
    void npcLocationChangeMovesCoLocatedUser() {
        var scene = new com.aditya.roleplay.model.Scene(
                "guild_hall", "guild_hall", "evening", java.util.List.of("aurora", "user"), "Quiet.", null);

        var updated = storyStateService.applyNpcLocationChange(scene, "training_ground");

        assertEquals("training_ground", updated.location());
        assertEquals("training_ground", updated.userLocation());
        assertNull(updated.currentSituation());
    }

    @Test
    void addsCharacterToSceneWithoutDuplicates() {
        var scene = new com.aditya.roleplay.model.Scene(
                "guild_hall", null, "evening", java.util.List.of("aurora", "user"), "Quiet.", null);

        var updated = storyStateService.applySceneChange(scene, new StateChange(
                StateChangeType.SCENE, "scene", "addCharacter", StateChangeOperation.SET, "laxus"));
        updated = storyStateService.applySceneChange(updated, new StateChange(
                StateChangeType.SCENE, "scene", "addCharacter", StateChangeOperation.SET, "laxus"));

        assertEquals(java.util.List.of("aurora", "user", "laxus"), updated.charactersPresent());
    }

    @Test
    void removesNpcButKeepsUserWhenCoLocated() {
        var scene = new com.aditya.roleplay.model.Scene(
                "guild_hall", null, "evening", java.util.List.of("aurora", "user", "laxus"), "Busy.", null);

        var updated = storyStateService.applySceneChange(scene, new StateChange(
                StateChangeType.SCENE, "scene", "removeCharacter", StateChangeOperation.SET, "laxus"));

        assertEquals(java.util.List.of("aurora", "user"), updated.charactersPresent());
    }

    @Test
    void canRemoveUserFromScene() {
        var scene = new com.aditya.roleplay.model.Scene(
                "guild_hall", "guild_hall", "evening", java.util.List.of("aurora", "user"), "Quiet.", null);

        var updated = storyStateService.applySceneChange(scene, new StateChange(
                StateChangeType.SCENE, "scene", "removeCharacter", StateChangeOperation.SET, "user"));

        assertEquals(java.util.List.of("aurora"), updated.charactersPresent());
        assertNull(updated.currentSituation());
    }

    @Test
    void clearsSituationWhenNpcLocationChanges() {
        var scene = new com.aditya.roleplay.model.Scene(
                "guild_hall", null, "evening", java.util.List.of("aurora", "user"), "Sharing a meal.", null);

        var updated = storyStateService.applySceneChange(scene, new StateChange(
                StateChangeType.SCENE, "scene", "location", StateChangeOperation.SET, "forest"));

        assertEquals("forest", updated.location());
        assertNull(updated.currentSituation());
    }

    @Test
    void appliesEmotionChange() {
        CharacterRuntimeState state = new CharacterRuntimeState(
                "aurora",
                new CharacterHealth(100, 100),
                "guild_hall",
                null,
                null);

        CharacterRuntimeState updated = storyStateService.applyEmotionChange(state, new StateChange(
                StateChangeType.EMOTION,
                "aurora",
                "emotion",
                StateChangeOperation.SET,
                "angry"));

        assertEquals("angry", updated.emotion());
    }
}
