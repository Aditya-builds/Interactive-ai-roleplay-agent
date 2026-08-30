package com.aditya.roleplay.service;

import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class StateChangeValidatorTest {

    @Inject
    StateChangeValidator validator;

    @Test
    void rejectsBatchExceedingMaxChangesPerTurn() {
        List<StateChange> tooMany = IntStream.range(0, 6)
                .mapToObj(i -> new StateChange(
                        StateChangeType.EMOTION,
                        "aurora",
                        "emotion",
                        StateChangeOperation.SET,
                        "calm"))
                .toList();

        StateChangeValidator.ValidationResult result = validator.validateBatch(
                tooMany, "aurora", Set.of("user"));

        assertFalse(result.valid());
    }

    @Test
    void rejectsRemovingUserFromScene() {
        StateChangeValidator.ValidationResult result = validator.validate(
                new StateChange(
                        StateChangeType.SCENE,
                        "scene",
                        "removeCharacter",
                        StateChangeOperation.SET,
                        "user"),
                "aurora",
                Set.of("user"));

        assertFalse(result.valid());
    }

    @Test
    void rejectsHealthChangeForWrongCharacter() {
        StateChangeValidator.ValidationResult result = validator.validate(
                new StateChange(
                        StateChangeType.HEALTH,
                        "laxus",
                        "current",
                        StateChangeOperation.DECREASE,
                        "10"),
                "aurora",
                Set.of("user", "laxus"));

        assertFalse(result.valid());
    }

    @Test
    void acceptsValidLocationChangeForActiveCharacter() {
        StateChangeValidator.ValidationResult result = validator.validate(
                new StateChange(
                        StateChangeType.LOCATION,
                        "aurora",
                        "location",
                        StateChangeOperation.SET,
                        "forest"),
                "aurora",
                Set.of("user"));

        assertTrue(result.valid());
    }
}
