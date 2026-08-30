package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.LlmException;
import com.aditya.roleplay.llm.TestLlmClient;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.storage.JsonStorageService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class RoleplayServiceTest {

    @Inject
    RoleplayService roleplayService;

    @Inject
    ConversationService conversationService;

    @Inject
    JsonStorageService storage;

    @Test
    void savesResponseAndRelationshipOnStructuredTurn() {
        TestLlmClient.structuredSuccess = true;

        Conversation conversation = conversationService.createConversation("aurora");
        roleplayService.processTurn(conversation.id(), "I step closer.");

        Conversation saved = storage.loadConversation(conversation.id()).orElseThrow();
        assertEquals(2, saved.messages().size());
        assertEquals("Aurora watches you carefully.", saved.messages().get(1).content());
        assertEquals(55, saved.relationship().familiarity());
    }

    @Test
    void doesNotSaveTurnWhenStructuredParsingFails() {
        TestLlmClient.structuredSuccess = false;

        Conversation conversation = conversationService.createConversation("aurora");

        assertThrows(LlmException.class, () -> roleplayService.processTurn(conversation.id(), "Hello"));

        Conversation saved = storage.loadConversation(conversation.id()).orElseThrow();
        assertEquals(0, saved.messages().size());
        assertEquals(54, saved.relationship().familiarity());
    }
}
