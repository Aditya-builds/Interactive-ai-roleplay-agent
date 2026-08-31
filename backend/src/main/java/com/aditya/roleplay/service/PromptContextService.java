package com.aditya.roleplay.service;

import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.model.StoryEvent;
import com.aditya.roleplay.model.StoryMemoryEntry;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PromptContextService {

    @ConfigProperty(name = "roleplay.prompt.recent-events-count", defaultValue = "5")
    int recentEventsCount;

    @ConfigProperty(name = "roleplay.prompt.important-memories-count", defaultValue = "8")
    int importantMemoriesCount;

    public String buildTurnContext(
            Conversation conversation,
            String latestUserMessage,
            Set<String> allowedRelationshipTargets) {
        return buildTurnContext(conversation, latestUserMessage, allowedRelationshipTargets, null, null);
    }

    public String buildTurnContext(
            Conversation conversation,
            String latestUserMessage,
            Set<String> allowedRelationshipTargets,
            PlayerPersona playerPersona,
            Story story) {

        String playerName = playerPersona != null ? playerPersona.name() : "the player";

        return """
                CURRENT SCENE
                %s

                %s CURRENT STATE
                %s

                RECENT STORY EVENTS
                %s

                IMPORTANT MEMORIES
                %s

                RELATIONSHIPS (%s's perspective — directional, not mutual)
                %s

                RECENT CONVERSATION
                (see message history above)

                PLAYER ACTION
                %s

                REMINDER: You control %s only. Never write for %s.
                If your response describes %s's mood or the scene dynamic differently from above, include matching stateChanges in your JSON.
                Return the structured JSON turn result now. Narrative goes in "response" only.
                """.formatted(
                formatCurrentSituation(conversation, playerPersona),
                conversation.characterId(),
                formatCharacterState(conversation),
                formatRecentEvents(conversation),
                formatImportantMemories(conversation),
                conversation.characterId(),
                formatRelationships(conversation, allowedRelationshipTargets),
                latestUserMessage,
                conversation.characterId(),
                playerName,
                conversation.characterId());
    }

    public String buildStateExtractionContext(
            Conversation conversation,
            String latestUserMessage,
            String narrative,
            Set<String> allowedRelationshipTargets,
            PlayerPersona playerPersona) {

        return """
                CURRENT SCENE
                %s

                %s CURRENT STATE
                %s

                RECENT STORY EVENTS
                %s

                IMPORTANT MEMORIES
                %s

                RELATIONSHIPS (%s's perspective — directional, not mutual)
                %s

                PLAYER ACTION
                %s

                LOCKED NARRATIVE (already shown to the player — do not rewrite)
                %s

                Extract stateChanges, events, and memories that match the locked narrative.
                If the narrative describes a mood, location, relationship, or scene shift, it MUST appear in stateChanges.
                Return structured JSON only — no narrative text.
                """.formatted(
                formatCurrentSituation(conversation, playerPersona),
                conversation.characterId(),
                formatCharacterState(conversation),
                formatRecentEvents(conversation),
                formatImportantMemories(conversation),
                conversation.characterId(),
                formatRelationships(conversation, allowedRelationshipTargets),
                latestUserMessage,
                narrative);
    }

    private String formatCharacterState(Conversation conversation) {
        var runtime = conversation.characterState();
        if (runtime == null) {
            return "Unknown";
        }
        var health = runtime.health() != null ? runtime.health() : new com.aditya.roleplay.model.CharacterHealth(100, 100);
        return """
                Health: %d/%d
                Location: %s
                Status: %s
                Emotion: %s
                """.formatted(
                health.current(),
                health.max(),
                runtime.location() != null ? runtime.location() : "unknown",
                runtime.status() != null ? runtime.status() : "none",
                runtime.emotion() != null ? runtime.emotion() : "none").stripTrailing();
    }

    private String formatCurrentSituation(Conversation conversation, PlayerPersona playerPersona) {
        var scene = conversation.scene();
        var runtime = conversation.characterState();
        var health = runtime != null && runtime.health() != null
                ? runtime.health()
                : new com.aditya.roleplay.model.CharacterHealth(100, 100);

        String present = String.join(", ", scene.charactersPresent());
        String conflict = scene.currentConflict() != null ? scene.currentConflict() : "none";
        String status = runtime != null && runtime.status() != null ? runtime.status() : "none";
        String emotion = runtime != null && runtime.emotion() != null ? runtime.emotion() : "none";
        String npcLocation = runtime != null && runtime.location() != null ? runtime.location() : scene.location();
        String userLocation = scene.userLocation();
        String situation = scene.currentSituation() != null ? scene.currentSituation() : "none";
        String playerLabel = playerPersona != null ? playerPersona.name() : "Player";

        return """
                NPC location: %s
                %s location: %s
                Time: %s
                Present with NPC: %s
                Situation: %s
                Conflict: %s
                %s health: %d/%d
                Status: %s
                Emotion: %s
                """.formatted(
                npcLocation,
                playerLabel,
                userLocation,
                scene.time(),
                present,
                situation,
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
                .limit(importantMemoriesCount * 2L)
                .toList();

        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<StoryMemoryEntry> deduped = ranked.stream()
                .filter(memory -> seen.add(memory.content().trim().toLowerCase()))
                .limit(importantMemoriesCount)
                .toList();

        if (deduped.isEmpty()) {
            return "- (none yet)";
        }

        return deduped.stream()
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

        String focalId = conversation.characterId();
        return conversation.relationships().stream()
                .filter(rel -> rel.sourceId() == null || focalId.equals(rel.sourceId()))
                .filter(rel -> allowedRelationshipTargets.contains(rel.targetId())
                        || "user".equals(rel.targetId())
                        || conversation.resolvedPlayerPersonaId().equals(rel.targetId()))
                .map(rel -> """
                        %s → %s: trust %d, respect %d, affection %d, familiarity %d, suspicion %d
                        """.formatted(
                        focalId,
                        rel.targetId(),
                        rel.trust(),
                        rel.respect(),
                        rel.affection(),
                        rel.familiarity(),
                        rel.suspicion()).strip())
                .collect(Collectors.joining("\n"));
    }
}
