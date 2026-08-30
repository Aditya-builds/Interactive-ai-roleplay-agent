package com.aditya.roleplay.service;

import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Scene;
import com.aditya.roleplay.model.World;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class PromptServiceTest {

    @Inject
    PromptService promptService;

    @Inject
    CharacterService characterService;

    @Test
    void respectsConfiguredMaxHistoryMessages() {
        RoleplayCharacter character = characterService.requireCharacter("aurora");
        World world = characterService.requireWorld(character.worldId());

        List<Message> messages = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            messages.add(new Message(UUID.randomUUID().toString(), Role.USER, "history-" + i, Instant.now()));
            messages.add(new Message(UUID.randomUUID().toString(), Role.ASSISTANT, "reply-" + i, Instant.now()));
        }

        Conversation conversation = new Conversation(
                UUID.randomUUID().toString(),
                "aurora",
                "fantasy",
                Instant.now(),
                Instant.now(),
                null,
                new Scene("guild_hall", null, "evening", List.of("aurora", "user"), "Quiet.", null),
                List.of(),
                List.of(),
                List.of(),
                messages);

        LlmRequest request = promptService.build(
                character, world, conversation, "latest-user-message", Set.of("user", "laxus"));

        long historyMessages = request.messages().stream()
                .filter(m -> m.content().startsWith("history-") || m.content().startsWith("reply-"))
                .count();

        assertTrue(historyMessages <= 20, "Should not exceed configured max-history-messages (20 in test profile)");
        assertEquals(20, historyMessages, "Should include exactly the last 20 transcript messages");
        assertTrue(request.messages().stream().anyMatch(m -> m.content().contains("CURRENT SCENE")));
    }
}
