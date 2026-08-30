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
                new CharacterRuntimeState("aurora", new CharacterHealth(100, 100), "guild_hall", null, null),
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
    void syncsSceneAndCharacterLocationOnLocationChange() {
        StateChangeProcessor.ConversationState initial = new StateChangeProcessor.ConversationState(
                "aurora",
                new CharacterRuntimeState("aurora", new CharacterHealth(100, 100), "guild_hall", null, null),
                new Scene("guild_hall", "evening", List.of("aurora", "user"), "Quiet.", null),
                List.of(new Relationship("user", 40, 50, 10, 20, 5)),
                List.of(),
                List.of());

        LlmTurnResult turnResult = new LlmTurnResult(
                "They travel.",
                List.of(new StateChange(
                        StateChangeType.LOCATION, "aurora", "location", StateChangeOperation.SET, "forest")),
                List.of(),
                List.of());

        StateChangeProcessor.ConversationState updated = processor.apply(
                initial, sampleCharacter(), turnResult, Set.of("user", "laxus"));

        assertEquals("forest", updated.scene().location());
        assertEquals("forest", updated.characterState().location());
    }

    @Test
    void appliesSceneLocationAndCharactersPresent() {
        StateChangeProcessor.ConversationState initial = new StateChangeProcessor.ConversationState(
                "aurora",
                new CharacterRuntimeState("aurora", new CharacterHealth(100, 100), "guild_hall", null, null),
                new Scene("guild_hall", "evening", List.of("aurora", "user"), "Quiet.", null),
                List.of(new Relationship("user", 40, 50, 10, 20, 5)),
                List.of(),
                List.of());

        LlmTurnResult turnResult = new LlmTurnResult(
                "They head out.",
                List.of(
                        new StateChange(StateChangeType.SCENE, "scene", "location", StateChangeOperation.SET, "forest"),
                        new StateChange(StateChangeType.SCENE, "scene", "addCharacter", StateChangeOperation.SET, "laxus")),
                List.of(),
                List.of());

        StateChangeProcessor.ConversationState updated = processor.apply(
                initial, sampleCharacter(), turnResult, Set.of("user", "laxus"));

        assertEquals("forest", updated.scene().location());
        assertEquals("forest", updated.characterState().location());
        assertEquals(List.of("aurora", "user", "laxus"), updated.scene().charactersPresent());
    }

    @Test
    void skipsDuplicateMemories() {
        StateChangeProcessor.ConversationState initial = new StateChangeProcessor.ConversationState(
                "aurora",
                new CharacterRuntimeState("aurora", new CharacterHealth(100, 100), "guild_hall", null, null),
                new Scene("guild_hall", "evening", List.of("aurora", "user"), "Quiet.", null),
                List.of(new Relationship("user", 40, 50, 10, 20, 5)),
                List.of(),
                List.of(new com.aditya.roleplay.model.StoryMemoryEntry(
                        "m1", "Same fact.", java.time.Instant.now(), "seed", 0.9, List.of(), List.of())));

        LlmTurnResult turnResult = new LlmTurnResult(
                "Noted.",
                List.of(),
                List.of(),
                List.of(new ProposedMemory("Same fact.", 0.95, List.of(), List.of())));

        StateChangeProcessor.ConversationState updated = processor.apply(
                initial, sampleCharacter(), turnResult, Set.of("user"));

        assertEquals(1, updated.memories().size());
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
