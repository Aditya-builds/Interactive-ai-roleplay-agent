package com.aditya.roleplay.service;

import com.aditya.roleplay.llm.LlmTurnResult;
import com.aditya.roleplay.llm.TestLlmClient;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import com.aditya.roleplay.storage.JsonStorageService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(RoleplayTwoPhaseTest.TwoPhaseProfile.class)
class RoleplayTwoPhaseTest {

    public static class TwoPhaseProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("roleplay.llm.two-phase", "true");
        }

        @Override
        public List<String> getEnabledAlternatives() {
            return List.of("com.aditya.roleplay.llm.TestLlmClient");
        }
    }

    @Inject
    RoleplayService roleplayService;

    @Inject
    ConversationService conversationService;

    @Inject
    JsonStorageService storage;

    @BeforeEach
    void resetLlm() {
        TestLlmClient.reset();
        TestLlmClient.structuredSuccess = true;
    }

    @Test
    void mergesNarrativeAndExtractionAcrossTwoCalls() {
        TestLlmClient.enqueue(new LlmTurnResult(
                "She leans closer, voice low.",
                List.of(),
                List.of(),
                List.of()));
        TestLlmClient.enqueue(new LlmTurnResult(
                "",
                List.of(new StateChange(
                        StateChangeType.RELATIONSHIP,
                        "user",
                        "familiarity",
                        StateChangeOperation.INCREASE,
                        "2")),
                List.of(),
                List.of()));

        Conversation conversation = conversationService.createConversation("aurora");
        roleplayService.processTurn(conversation.id(), "I step closer.");

        Conversation saved = storage.loadConversation(conversation.id()).orElseThrow();
        assertEquals(3, saved.messages().size());
        assertEquals("She leans closer, voice low.", saved.messages().get(2).content());
        assertEquals(56, saved.userRelationship().familiarity());
    }
}
