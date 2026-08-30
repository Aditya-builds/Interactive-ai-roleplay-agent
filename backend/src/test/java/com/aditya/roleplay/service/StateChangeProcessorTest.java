package com.aditya.roleplay.service;

import com.aditya.roleplay.llm.LlmTurnResult;
import com.aditya.roleplay.model.CharacterHealth;
import com.aditya.roleplay.model.CharacterRuntimeState;
import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Scene;
import com.aditya.roleplay.model.turn.ProposedMemory;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class StateChangeProcessorTest {

    @Inject
    StateChangeProcessor processor;

    @Inject
    StateChangeValidator validator;

    @Test
    void appliesValidRelationshipChangeAndMemory() {
        RoleplayCharacter character = sampleCharacter();
        StateChangeProcessor.ConversationState initial = new StateChangeProcessor.ConversationState(
                "aurora",
                new CharacterRuntimeState("aurora", new CharacterHealth(100, 100), null, null),
                new Scene("guild_hall", "evening", List.of("aurora", "user"), "Quiet.", null),
                List.of(new Relationship("user", 40, 50, 10, 20, 5)),
                List.of(),
                List.of());

        LlmTurnResult turnResult = new LlmTurnResult(
                "Aurora nods.",
                List.of(new StateChange(
                        StateChangeType.RELATIONSHIP,
                        "user",
                        "respect",
                        StateChangeOperation.INCREASE,
                        "2")),
                List.of(),
                List.of(new ProposedMemory("Aurora acknowledged the challenge.", 0.8, List.of("respect"), List.of("user"))));

        StateChangeProcessor.ConversationState updated = processor.apply(
                initial, character, turnResult, Set.of("user", "laxus"));

        assertEquals(52, updated.relationships().get(0).respect());
        assertEquals(1, updated.memories().size());
        assertEquals("llm", updated.memories().get(0).source());
    }

    @Test
    void appliesCrossCharacterRelationshipChange() {
        StateChangeProcessor.ConversationState initial = new StateChangeProcessor.ConversationState(
                "aurora",
                new CharacterRuntimeState("aurora", new CharacterHealth(100, 100), null, null),
                new Scene("guild_hall", "evening", List.of("aurora", "user"), "Quiet.", null),
                List.of(
                        new Relationship("user", 40, 50, 10, 20, 5),
                        new Relationship("laxus", 35, 72, 5, 48, 15)),
                List.of(),
                List.of());

        LlmTurnResult turnResult = new LlmTurnResult(
                "Aurora nods at Laxus.",
                List.of(new StateChange(
                        StateChangeType.RELATIONSHIP,
                        "laxus",
                        "trust",
                        StateChangeOperation.INCREASE,
                        "3")),
                List.of(),
                List.of());

        StateChangeProcessor.ConversationState updated = processor.apply(
                initial, sampleCharacter(), turnResult, Set.of("user", "laxus"));

        Relationship withLaxus = updated.relationships().stream()
                .filter(r -> "laxus".equals(r.targetId()))
                .findFirst()
                .orElseThrow();
        assertEquals(38, withLaxus.trust());
    }

    @Test
    void appliesSceneLocationAndCharactersPresent() {
        StateChangeProcessor.ConversationState initial = new StateChangeProcessor.ConversationState(
                "aurora",
                new CharacterRuntimeState("aurora", new CharacterHealth(100, 100), null, null),
                new Scene("guild_hall", "evening", List.of("aurora", "user"), "Quiet.", null),
                List.of(new Relationship("user", 40, 50, 10, 20, 5)),
                List.of(),
                List.of());

        LlmTurnResult turnResult = new LlmTurnResult(
                "They head out.",
                List.of(
                        new StateChange(StateChangeType.SCENE, "scene", "location", StateChangeOperation.SET, "forest"),
                        new StateChange(StateChangeType.SCENE, "scene", "charactersPresent", StateChangeOperation.SET, "[\"aurora\",\"user\",\"laxus\"]")),
                List.of(),
                List.of());

        StateChangeProcessor.ConversationState updated = processor.apply(
                initial, sampleCharacter(), turnResult, Set.of("user", "laxus"));

        assertEquals("forest", updated.scene().location());
        assertEquals(List.of("aurora", "user", "laxus"), updated.scene().charactersPresent());
    }

    @Test
    void rejectsInvalidTargetWithoutApplyingChange() {
        StateChangeValidator.ValidationResult result = validator.validate(
                new StateChange(
                        StateChangeType.RELATIONSHIP,
                        "unknown_npc",
                        "trust",
                        StateChangeOperation.INCREASE,
                        "2"),
                "aurora",
                Set.of("user", "laxus"));

        assertFalse(result.valid());
    }

    @Test
    void rejectsUnsupportedField() {
        StateChangeValidator.ValidationResult result = validator.validate(
                new StateChange(
                        StateChangeType.RELATIONSHIP,
                        "user",
                        "charisma",
                        StateChangeOperation.INCREASE,
                        "2"),
                "aurora",
                Set.of("user"));

        assertFalse(result.valid());
    }

    @Test
    void rejectsOversizedRelationshipDelta() {
        StateChangeValidator.ValidationResult result = validator.validate(
                new StateChange(
                        StateChangeType.RELATIONSHIP,
                        "user",
                        "trust",
                        StateChangeOperation.INCREASE,
                        "50"),
                "aurora",
                Set.of("user"));

        assertFalse(result.valid());
    }

    private RoleplayCharacter sampleCharacter() {
        return new RoleplayCharacter(
                "aurora",
                "fantasy",
                "Aurora",
                "/characters/aurora.png",
                new CharacterHealth(100, 100),
                List.of("calm"),
                "Background",
                "Direct",
                List.of("loyalty"),
                null,
                List.of(),
                List.of());
    }
}
