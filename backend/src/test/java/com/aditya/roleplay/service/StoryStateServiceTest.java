package com.aditya.roleplay.service;

import com.aditya.roleplay.model.CharacterHealth;
import com.aditya.roleplay.model.CharacterRuntimeState;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void appliesHealingWithoutExceedingMaxHealth() {
        CharacterRuntimeState state = new CharacterRuntimeState(
                "aurora",
                new CharacterHealth(90, 100),
                "guild_hall",
                null,
                null);

        CharacterRuntimeState updated = storyStateService.applyHealthChange(state, new StateChange(
                StateChangeType.HEALTH,
                "aurora",
                "current",
                StateChangeOperation.INCREASE,
                "20"));

        assertEquals(100, updated.health().current());
    }
}
