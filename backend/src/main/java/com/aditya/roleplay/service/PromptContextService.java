package com.aditya.roleplay.service;

import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.StoryEvent;
import com.aditya.roleplay.model.StoryMemoryEntry;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PromptContextService {

    @ConfigProperty(name = "roleplay.prompt.recent-events-count", defaultValue = "5")
    int recentEventsCount;

    @ConfigProperty(name = "roleplay.prompt.important-memories-count", defaultValue = "8")
    int importantMemoriesCount;

    public String buildTurnContext(Conversation conversation, String latestUserMessage, Set<String> allowedRelationshipTargets) {
        return """
                CURRENT SITUATION
                %s

                RECENT EVENTS
                %s

                IMPORTANT MEMORIES
                %s

                RELATIONSHIPS
                %s

                USER MESSAGE
                %s

                Return the structured JSON turn result now. Narrative goes in "response" only.
                """.formatted(
                formatCurrentSituation(conversation),
                formatRecentEvents(conversation),
                formatImportantMemories(conversation),
                formatRelationships(conversation, allowedRelationshipTargets),
                latestUserMessage);
    }

    private String formatCurrentSituation(Conversation conversation) {
        var scene = conversation.scene();
        var runtime = conversation.characterState();
        var health = runtime != null && runtime.health() != null
                ? runtime.health()
                : new com.aditya.roleplay.model.CharacterHealth(100, 100);

        String present = String.join(", ", scene.charactersPresent());
        String conflict = scene.currentConflict() != null ? scene.currentConflict() : "none";
        String status = runtime != null && runtime.status() != null ? runtime.status() : "none";
        String emotion = runtime != null && runtime.emotion() != null ? runtime.emotion() : "none";

        return """
                Location: %s
                Time: %s
                Present: %s
                Situation: %s
                Conflict: %s
                %s health: %d/%d
                Status: %s
                Emotion: %s
                """.formatted(
                scene.location(),
                scene.time(),
                present,
                scene.currentSituation(),
                conflict,
                conversation.characterId(),
                health.current(),
                health.max(),
                status,
                emotion).stripTrailing();
    }

    private String formatRecentEvents(Conversation conversation) {
        List<StoryEvent> recent = conversation.events().stream()
                .sorted(Comparator.comparing(StoryEvent::createdAt).reversed())
                .limit(recentEventsCount)
                .toList();

        if (recent.isEmpty()) {
            return "- (none yet)";
        }

        return recent.stream()
                .map(event -> "- " + event.description()
                        + (event.participants().isEmpty() ? "" : " [" + String.join(", ", event.participants()) + "]"))
                .collect(Collectors.joining("\n"));
    }

    private String formatImportantMemories(Conversation conversation) {
        List<StoryMemoryEntry> ranked = conversation.memories().stream()
                .sorted(Comparator
                        .comparing((StoryMemoryEntry m) -> m.importance() != null ? m.importance() : 0.0)
                        .reversed()
                        .thenComparing(StoryMemoryEntry::createdAt, Comparator.reverseOrder()))
                .limit(importantMemoriesCount)
                .toList();

        if (ranked.isEmpty()) {
            return "- (none yet)";
        }

        return ranked.stream()
                .map(memory -> {
                    String tags = memory.tags().isEmpty() ? "" : " (" + String.join(", ", memory.tags()) + ")";
                    return "- " + memory.content() + tags;
                })
                .collect(Collectors.joining("\n"));
    }

    private String formatRelationships(Conversation conversation, Set<String> allowedRelationshipTargets) {
        if (conversation.relationships().isEmpty()) {
            return "- (none tracked)";
        }

        return conversation.relationships().stream()
                .filter(rel -> allowedRelationshipTargets.contains(rel.targetId()) || "user".equals(rel.targetId()))
                .map(rel -> """
                        With %s: trust %d, respect %d, affection %d, familiarity %d, suspicion %d
                        """.formatted(
                        rel.targetId(),
                        rel.trust(),
                        rel.respect(),
                        rel.affection(),
                        rel.familiarity(),
                        rel.suspicion()).strip())
                .collect(Collectors.joining("\n"));
    }
}
