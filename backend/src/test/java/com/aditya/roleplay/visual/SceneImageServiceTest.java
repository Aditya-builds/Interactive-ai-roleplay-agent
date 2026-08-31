package com.aditya.roleplay.visual;

import com.aditya.roleplay.llm.TestLlmClient;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.visual.GenerateSceneImageResponse;
import com.aditya.roleplay.storage.JsonStorageService;
import com.aditya.roleplay.service.ConversationService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SceneImageServiceTest {

    @Inject
    SceneImageService sceneImageService;

    @Inject
    ConversationService conversationService;

    @Inject
    JsonStorageService storage;

    @BeforeEach
    void resetLlm() {
        TestLlmClient.reset();
    }

    @Test
    void generatesSceneImageAndAppendsMessage() {
        Conversation conversation = conversationService.createConversation("aurora");
        int initialMessages = conversation.messages().size();

        GenerateSceneImageResponse response = sceneImageService.generateForConversation(conversation.id());

        assertNotNull(response.sceneImage());
        assertNotNull(response.sceneImageMessage().sceneImageId());
        assertTrue(response.sceneImage().prompt().contains("CHARACTER IDENTITY (LOCKED"));
        assertTrue(response.sceneImage().imageUrl().startsWith("/api/scene-images/"));

        Conversation saved = storage.loadConversation(conversation.id()).orElseThrow();
        assertEquals(initialMessages + 1, saved.messages().size());
        assertTrue(saved.messages().get(saved.messages().size() - 1).isSceneImage());
    }
}
