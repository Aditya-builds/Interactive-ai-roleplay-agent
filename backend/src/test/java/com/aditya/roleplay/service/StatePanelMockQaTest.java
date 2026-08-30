package com.aditya.roleplay.service;

import com.aditya.roleplay.llm.LlmTurnResult;
import com.aditya.roleplay.llm.TestLlmClient;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.SendMessageResponse;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Mock QA scenario: verifies scene / character / relationship state is returned
 * and persisted after each turn — the same data the Angular state panel displays.
 */
@QuarkusTest
class StatePanelMockQaTest {

    @Inject
    RoleplayService roleplayService;

    @Inject
    ConversationService conversationService;

    @BeforeEach
    void resetLlm() {
        TestLlmClient.reset();
        TestLlmClient.structuredSuccess = true;
    }

    @Test
    @DisplayName("Mock QA: caring message updates trust, emotion, status in API response")
    void caringMessageUpdatesStatePanelFields() {
        Conversation conversation = conversationService.createConversation("aurora");
        int initialTrust = conversation.userRelationship().trust();
        assertEquals(42, initialTrust);

        TestLlmClient.enqueue(new LlmTurnResult(
                "Aurora offers a faint smile. 'I appreciate you asking,' she replies calmly.",
                List.of(
                        new StateChange(StateChangeType.RELATIONSHIP, "user", "trust", StateChangeOperation.INCREASE, "2"),
                        new StateChange(StateChangeType.RELATIONSHIP, "user", "affection", StateChangeOperation.INCREASE, "1"),
                        new StateChange(StateChangeType.EMOTION, "aurora", "emotion", StateChangeOperation.SET, "grateful"),
                        new StateChange(StateChangeType.STATUS, "aurora", "status", StateChangeOperation.SET, "exhausted"),
                        new StateChange(StateChangeType.SCENE, "scene", "currentSituation", StateChangeOperation.SET,
                                "Aurora is opening up about her fatigue.")),
                List.of(),
                List.of()));

        SendMessageResponse response = roleplayService.processTurn(
                conversation.id(), "you look tired are you alright ?");

        assertNotNull(response.scene());
        assertNotNull(response.characterState());
        assertNotNull(response.relationships());

        assertEquals("grateful", response.characterState().emotion());
        assertEquals("exhausted", response.characterState().status());
        assertEquals(100, response.characterState().health().current());
        assertEquals("Aurora is opening up about her fatigue.", response.scene().currentSituation());

        var userRel = response.relationships().stream()
                .filter(r -> "user".equals(r.targetId()))
                .findFirst()
                .orElseThrow();
        assertEquals(initialTrust + 2, userRel.trust());
        assertEquals(13, userRel.affection());

        Conversation persisted = conversationService.getConversation(conversation.id());
        assertEquals("grateful", persisted.characterState().emotion());
        assertEquals(initialTrust + 2, persisted.userRelationship().trust());
        assertEquals(3, persisted.messages().size());
    }

    @Test
    @DisplayName("Mock QA: hand-holding updates emotion and situation")
    void handHoldingUpdatesEmotionAndSituation() {
        Conversation conversation = conversationService.createConversation("aurora");

        TestLlmClient.enqueue(new LlmTurnResult(
                "Aurora's expression softens. \"Your concern means more than you might think.\"",
                List.of(
                        new StateChange(StateChangeType.RELATIONSHIP, "user", "affection", StateChangeOperation.INCREASE, "2"),
                        new StateChange(StateChangeType.EMOTION, "aurora", "emotion", StateChangeOperation.SET, "grateful"),
                        new StateChange(StateChangeType.SCENE, "scene", "currentSituation", StateChangeOperation.SET,
                                "A quiet personal moment in the guild hall.")),
                List.of(),
                List.of()));

        SendMessageResponse response = roleplayService.processTurn(
                conversation.id(), "i hold her hand gently and asked to take care of herself");

        assertEquals("grateful", response.characterState().emotion());
        assertEquals("A quiet personal moment in the guild hall.", response.scene().currentSituation());
        assertEquals(14, response.relationships().stream().filter(r -> "user".equals(r.targetId())).findFirst().orElseThrow().affection());
    }

    @Test
    @DisplayName("Mock QA: location and health changes appear in REST send response")
    void restEndpointReturnsUpdatedStateForStatePanel() {
        String conversationId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("characterId", "aurora"))
                .when()
                .post("/api/conversations")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .extract()
                .path("id");

        TestLlmClient.enqueue(new LlmTurnResult(
                "The blow lands hard at the training ground.",
                List.of(
                        new StateChange(StateChangeType.SCENE, "scene", "location", StateChangeOperation.SET, "training_ground"),
                        new StateChange(StateChangeType.HEALTH, "aurora", "current", StateChangeOperation.DECREASE, "15"),
                        new StateChange(StateChangeType.EMOTION, "aurora", "emotion", StateChangeOperation.SET, "irritated"),
                        new StateChange(StateChangeType.STATUS, "aurora", "status", StateChangeOperation.SET, "injured"),
                        new StateChange(StateChangeType.RELATIONSHIP, "user", "respect", StateChangeOperation.INCREASE, "2")),
                List.of(),
                List.of()));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("content", "I spar with Aurora and land a heavy hit."))
                .when()
                .post("/api/conversations/{id}/messages", conversationId)
                .then()
                .statusCode(200)
                .body("scene.location", equalTo("training_ground"))
                .body("characterState.health.current", equalTo(85))
                .body("characterState.emotion", equalTo("irritated"))
                .body("characterState.status", equalTo("injured"))
                .body("relationships.find { it.targetId == 'user' }.respect", equalTo(69));

        given()
                .when()
                .get("/api/conversations/{id}", conversationId)
                .then()
                .statusCode(200)
                .body("characterState.health.current", equalTo(85))
                .body("scene.location", equalTo("training_ground"));
    }
}
