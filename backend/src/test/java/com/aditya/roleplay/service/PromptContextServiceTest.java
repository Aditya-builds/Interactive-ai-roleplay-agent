package com.aditya.roleplay.service;

import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.Scene;
import com.aditya.roleplay.model.StoryEvent;
import com.aditya.roleplay.model.StoryMemoryEntry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class PromptContextServiceTest {

    @Inject
    PromptContextService promptContextService;

    @Test
    void includesRecentEventsAndImportantMemories() {
        Instant now = Instant.now();
        Conversation conversation = new Conversation(
                "id",
                "aurora",
                "fantasy",
                now,
                now,
                null,
                new Scene("guild_hall", null, "evening", List.of("aurora", "user"), "Waiting.", null),
                List.of(new Relationship("user", 42, 67, 12, 54, 8)),
                List.of(new StoryEvent("e1", "Aurora was ambushed.", now, List.of("aurora"), 0.9)),
                List.of(
                        new StoryMemoryEntry("m1", "Low importance.", now, "seed", 0.2, List.of(), List.of()),
                        new StoryMemoryEntry("m2", "Forest mission rescue.", now, "seed", 0.9, List.of("mission"), List.of("user"))),
                List.of());

        String context = promptContextService.buildTurnContext(conversation, "Hello", Set.of("user", "laxus"));

        assertTrue(context.contains("CURRENT SCENE"));
        assertTrue(context.contains("RECENT STORY EVENTS"));
        assertTrue(context.contains("Aurora was ambushed."));
        assertTrue(context.contains("Forest mission rescue."));
        assertTrue(context.contains("RELATIONSHIPS"));
        assertTrue(context.contains("aurora → user:"));
    }
}
