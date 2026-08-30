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
                new CharacterRuntimeState("aurora", new CharacterHealth(100, 100), "guild_hall", null, null),
                new Scene("guild_hall", "evening", List.of("aurora", "user"), "Quiet.", null),
                new Relationship("aurora", 40, 50, 10, 20, 5),
                List.of(),
                List.of());

        LlmTurnResult turnResult = new LlmTurnResult(
                "Aurora nods.",
                List.of(new StateChange(
                        StateChangeType.RELATIONSHIP,
                        "aurora",
                        "respect",
                        StateChangeOperation.INCREASE,
                        "2")),
                List.of(),
                List.of(new ProposedMemory("Aurora acknowledged the challenge.", 0.8)));

        StateChangeProcessor.ConversationState updated = processor.apply(initial, character, turnResult);

        assertEquals(52, updated.relationship().respect());
        assertEquals(1, updated.memories().size());
        assertEquals("llm", updated.memories().get(0).source());
    }

    @Test
    void rejectsInvalidTargetWithoutApplyingChange() {
        StateChangeValidator.ValidationResult result = validator.validate(
                new StateChange(
                        StateChangeType.RELATIONSHIP,
                        "laxus",
                        "trust",
                        StateChangeOperation.INCREASE,
                        "2"),
                "aurora");

        assertFalse(result.valid());
    }

    @Test
    void rejectsUnsupportedField() {
        StateChangeValidator.ValidationResult result = validator.validate(
                new StateChange(
                        StateChangeType.RELATIONSHIP,
                        "aurora",
                        "charisma",
                        StateChangeOperation.INCREASE,
                        "2"),
                "aurora");

        assertFalse(result.valid());
    }

    @Test
    void rejectsOversizedRelationshipDelta() {
        StateChangeValidator.ValidationResult result = validator.validate(
                new StateChange(
                        StateChangeType.RELATIONSHIP,
                        "aurora",
                        "trust",
                        StateChangeOperation.INCREASE,
                        "50"),
                "aurora");

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
                null);
    }
}
