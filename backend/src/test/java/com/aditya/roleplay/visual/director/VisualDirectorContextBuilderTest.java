package com.aditya.roleplay.visual.director;

import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.visual.director.VisualDirectorRequest;
import com.aditya.roleplay.service.CharacterService;
import com.aditya.roleplay.service.ConversationService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class VisualDirectorContextBuilderTest {

    @Inject
    VisualDirectorContextBuilder contextBuilder;

    @Inject
    ConversationService conversationService;

    @Inject
    CharacterService characterService;

    @BeforeEach
    void reset() {
        // no-op
    }

    @Test
    void buildsCompactDirectorRequestFromConversation() {
        Conversation conversation = conversationService.createConversation("aurora");
        var character = characterService.requireCharacter("aurora");
        var world = characterService.requireWorld(conversation.worldId());

        VisualDirectorRequest request = contextBuilder.build(conversation, character, world, true);

        assertEquals(conversation.id(), request.conversationId());
        assertEquals("aurora", request.focalCharacterId());
        assertTrue(request.explicitGeneration());
        assertFalse(request.recentMessages().isEmpty());
        assertTrue(request.visualIdentities().stream().anyMatch(id -> "aurora".equals(id.characterId())));
        assertTrue(request.candidateCharacters().stream().anyMatch(c -> "aurora".equals(c.characterId())));
    }

    @Test
    void excludesSceneImageMessagesFromRecentMessages() {
        Conversation conversation = conversationService.createConversation("aurora");
        Conversation withSceneImage = conversation.appendMessage(new Message(
                "scene-msg",
                Role.ASSISTANT,
                "Scene image",
                Instant.now(),
                "img-1"));
        var character = characterService.requireCharacter("aurora");
        var world = characterService.requireWorld(conversation.worldId());

        VisualDirectorRequest request = contextBuilder.build(withSceneImage, character, world, true);

        assertTrue(request.recentMessages().stream().noneMatch(message -> "scene-msg".equals(message.id())));
    }
}
