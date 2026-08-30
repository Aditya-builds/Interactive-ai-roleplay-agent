package com.aditya.roleplay.service;

import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.llm.LlmTurnResult;
import com.aditya.roleplay.llm.TestLlmClient;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.turn.ProposedMemory;
import com.aditya.roleplay.model.turn.ProposedStoryEvent;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import com.aditya.roleplay.storage.JsonStorageService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class RoleplayMultiTurnTest {

    @Inject
    RoleplayService roleplayService;

    @Inject
    PromptService promptService;

    @Inject
    PromptContextService promptContextService;

    @Inject
    ConversationService conversationService;

    @Inject
    CharacterService characterService;

    @Inject
    JsonStorageService storage;

    @BeforeEach
    void resetLlm() {
        TestLlmClient.reset();
        TestLlmClient.structuredSuccess = true;
    }

    @Test
    void multiTurnConversationEvolvesStateAndPromptContext() {
        TestLlmClient.enqueue(new LlmTurnResult(
                "The guild hall is quiet.",
                List.of(new StateChange(
                        StateChangeType.SCENE, "scene", "location", StateChangeOperation.SET, "guild_hall")),
                List.of(),
                List.of()));

        Conversation conversation = conversationService.createConversation("aurora");
        roleplayService.processTurn(conversation.id(), "I enter the guild hall.");

        Conversation afterTurn1 = storage.loadConversation(conversation.id()).orElseThrow();
        assertEquals("guild_hall", afterTurn1.scene().location());
        assertEquals("guild_hall", afterTurn1.characterState().location());

        TestLlmClient.enqueue(new LlmTurnResult(
                "Aurora's eyes narrow with interest.",
                List.of(
                        new StateChange(StateChangeType.RELATIONSHIP, "user", "respect", StateChangeOperation.INCREASE, "3"),
                        new StateChange(StateChangeType.EMOTION, "aurora", "emotion", StateChangeOperation.SET, "focused")),
                List.of(new ProposedStoryEvent("The user challenged Aurora to a fight.", 0.7, List.of("aurora", "user"))),
                List.of()));

        roleplayService.processTurn(conversation.id(), "I challenge Aurora to a fight.");
        Conversation afterTurn2 = storage.loadConversation(conversation.id()).orElseThrow();
        assertEquals("focused", afterTurn2.characterState().emotion());
        assertEquals(70, afterTurn2.userRelationship().respect());
        assertEquals(1, afterTurn2.events().size());

        TestLlmClient.enqueue(new LlmTurnResult(
                "Aurora staggers from the blow.",
                List.of(new StateChange(
                        StateChangeType.HEALTH, "aurora", "current", StateChangeOperation.DECREASE, "15")),
                List.of(new ProposedStoryEvent("The user attacked Aurora.", 0.8, List.of("aurora", "user"))),
                List.of(new ProposedMemory("The user struck Aurora during the spar.", 0.75, List.of("combat"), List.of("user", "aurora")))));

        roleplayService.processTurn(conversation.id(), "I attack Aurora.");
        Conversation afterTurn3 = storage.loadConversation(conversation.id()).orElseThrow();
        assertEquals(85, afterTurn3.characterState().health().current());
        assertTrue(afterTurn3.events().stream().anyMatch(e -> e.description().contains("attacked Aurora")));
        assertTrue(afterTurn3.memories().stream().anyMatch(m -> m.content().contains("struck Aurora")));

        TestLlmClient.enqueue(new LlmTurnResult(
                "Aurora exhales, tension easing.",
                List.of(
                        new StateChange(StateChangeType.RELATIONSHIP, "user", "trust", StateChangeOperation.INCREASE, "3"),
                        new StateChange(StateChangeType.RELATIONSHIP, "user", "suspicion", StateChangeOperation.DECREASE, "2")),
                List.of(),
                List.of()));

        roleplayService.processTurn(conversation.id(), "I apologize.");
        Conversation afterTurn4 = storage.loadConversation(conversation.id()).orElseThrow();
        assertEquals(45, afterTurn4.userRelationship().trust());
        assertEquals(6, afterTurn4.userRelationship().suspicion());

        TestLlmClient.enqueue(new LlmTurnResult(
                "They leave the hall for the forest.",
                List.of(
                        new StateChange(StateChangeType.LOCATION, "aurora", "location", StateChangeOperation.SET, "forest"),
                        new StateChange(StateChangeType.SCENE, "scene", "addCharacter", StateChangeOperation.SET, "laxus")),
                List.of(),
                List.of()));

        roleplayService.processTurn(conversation.id(), "We head to the forest.");
        Conversation afterTurn5 = storage.loadConversation(conversation.id()).orElseThrow();
        assertEquals("forest", afterTurn5.scene().location());
        assertEquals("forest", afterTurn5.characterState().location());
        assertTrue(afterTurn5.scene().charactersPresent().contains("laxus"));
        assertTrue(afterTurn5.scene().charactersPresent().contains("user"));

        String context = promptContextService.buildTurnContext(
                afterTurn5, "What happens next?", Set.of("user", "laxus"));
        assertTrue(context.contains("RECENT STORY EVENTS"));
        assertTrue(context.contains("attacked Aurora"));
        assertTrue(context.contains("forest"));
        assertTrue(context.contains("85/100"));
        assertTrue(context.contains("With user: trust 45"));
        assertTrue(context.contains("struck Aurora"));
        assertEquals(10, afterTurn5.messages().size());
    }

    @Test
    void doesNotSaveTurnWhenStructuredOutputFails() {
        TestLlmClient.structuredSuccess = false;
        Conversation conversation = conversationService.createConversation("aurora");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.aditya.roleplay.exception.LlmException.class,
                () -> roleplayService.processTurn(conversation.id(), "Hello"));

        Conversation saved = storage.loadConversation(conversation.id()).orElseThrow();
        assertEquals(0, saved.messages().size());
    }

    @Test
    void enforcesMaxFiveStateChangesPerTurn() {
        TestLlmClient.enqueue(new LlmTurnResult(
                "Many small shifts.",
                List.of(
                        new StateChange(StateChangeType.EMOTION, "aurora", "emotion", StateChangeOperation.SET, "a"),
                        new StateChange(StateChangeType.EMOTION, "aurora", "emotion", StateChangeOperation.SET, "b"),
                        new StateChange(StateChangeType.EMOTION, "aurora", "emotion", StateChangeOperation.SET, "c"),
                        new StateChange(StateChangeType.EMOTION, "aurora", "emotion", StateChangeOperation.SET, "d"),
                        new StateChange(StateChangeType.EMOTION, "aurora", "emotion", StateChangeOperation.SET, "e"),
                        new StateChange(StateChangeType.EMOTION, "aurora", "emotion", StateChangeOperation.SET, "f")),
                List.of(),
                List.of()));

        Conversation conversation = conversationService.createConversation("aurora");
        roleplayService.processTurn(conversation.id(), "Trigger many changes.");

        Conversation saved = storage.loadConversation(conversation.id()).orElseThrow();
        assertEquals("e", saved.characterState().emotion());
    }
}
