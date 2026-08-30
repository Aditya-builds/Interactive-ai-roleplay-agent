package com.aditya.roleplay.service;

import com.aditya.roleplay.llm.LlmMessage;
import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.model.CharacterHealth;
import com.aditya.roleplay.model.CharacterRuntimeState;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Scene;
import com.aditya.roleplay.model.StoryMemoryEntry;
import com.aditya.roleplay.model.World;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class PromptService {

    @ConfigProperty(name = "roleplay.llm.temperature")
    double temperature;

    @ConfigProperty(name = "roleplay.llm.max-tokens")
    int maxTokens;

    @ConfigProperty(name = "roleplay.prompt.max-history-messages")
    int maxHistoryMessages;

    @ConfigProperty(name = "roleplay.llm.json-mode", defaultValue = "true")
    boolean jsonMode;

    public LlmRequest build(
            RoleplayCharacter character,
            World world,
            Conversation conversation,
            String latestUserMessage) {

        String systemPrompt = buildSystemPrompt(character, world);
        List<Message> recentMessages = selectRecentMessages(conversation.messages());
        List<LlmMessage> chatMessages = buildChatMessages(recentMessages, character.name(), latestUserMessage, conversation);

        return new LlmRequest(systemPrompt, chatMessages, temperature, maxTokens, jsonMode);
    }

    private String buildSystemPrompt(RoleplayCharacter character, World world) {
        String personality = String.join(", ", character.personality());
        String values = character.values().stream()
                .map(v -> "- " + v)
                .collect(Collectors.joining("\n"));
        String rules = world.rules().stream()
                .map(r -> "- " + r)
                .collect(Collectors.joining("\n"));

        return """
                You are %s, a fictional character in a roleplay story.

                CHARACTER PROFILE
                Name: %s
                Personality: %s
                Background: %s
                Speaking style: %s
                Values:
                %s

                WORLD
                %s: %s
                Rules:
                %s

                ROLEPLAY RULES (CRITICAL)
                1. Stay in character at all times.
                2. You control ONLY %s, NPCs, and the environment.
                3. NEVER control the user's character: do not describe what the user does, thinks, feels, or says unless they explicitly wrote it.
                4. Do not write the user's dialogue or internal monologue.
                5. Maintain continuity with the scene, memories, and recent conversation.
                6. Do not invent contradictory facts about the character or world.
                7. Do not force romantic progression; let relationships develop naturally.
                8. Only propose state changes justified by the current interaction.
                9. Keep narrative responses concise: 1-4 short paragraphs unless the moment demands more.
                10. Return ONLY valid JSON matching the schema below. No markdown, no extra text.

                STRUCTURED OUTPUT SCHEMA
                Return a single JSON object with these fields:

                {
                  "response": "Narrative reply the player sees, written in character.",
                  "stateChanges": [
                    {
                      "type": "RELATIONSHIP | SCENE | HEALTH | LOCATION | STATUS | EMOTION",
                      "targetId": "character id (e.g. %s) or 'scene' for scene changes",
                      "field": "field name",
                      "operation": "SET | INCREASE | DECREASE",
                      "value": "string value or numeric amount as string"
                    }
                  ],
                  "events": [
                    {
                      "description": "What happened this turn",
                      "importance": 0.0,
                      "participants": ["character ids or user"]
                    }
                  ],
                  "memories": [
                    {
                      "content": "Important fact worth remembering",
                      "importance": 0.0
                    }
                  ]
                }

                STATE CHANGE RULES
                - RELATIONSHIP targetId must be %s. Fields: trust, respect, affection, familiarity, suspicion. Use INCREASE/DECREASE with small values (1-5) or SET (0-100).
                - SCENE targetId must be "scene". Fields: location, time, currentSituation, currentConflict. Use SET only. Location must be lowercase slug (e.g. guild_hall, forest).
                - HEALTH targetId must be %s. Field: current. Use INCREASE/DECREASE/SET. Do not change max health.
                - LOCATION targetId must be %s. Field: location. Use SET only.
                - STATUS targetId must be %s. Field: status. Use SET only (e.g. injured, exhausted, or null).
                - EMOTION targetId must be %s. Field: emotion. Use SET only.
                - Propose at most 5 stateChanges per turn.
                - Use empty arrays when nothing applies.
                """.formatted(
                character.name(),
                character.name(),
                personality,
                character.background(),
                character.speakingStyle(),
                values,
                world.name(),
                world.description(),
                rules,
                character.name(),
                character.id(),
                character.id(),
                character.id(),
                character.id(),
                character.id(),
                character.id());
    }

    private List<Message> selectRecentMessages(List<Message> messages) {
        if (messages.size() <= maxHistoryMessages) {
            return messages;
        }
        return messages.subList(messages.size() - maxHistoryMessages, messages.size());
    }

    private List<LlmMessage> buildChatMessages(
            List<Message> recentMessages,
            String characterName,
            String latestUserMessage,
            Conversation conversation) {

        List<LlmMessage> result = new ArrayList<>();

        int start = Math.max(0, recentMessages.size() - 12);
        for (Message message : recentMessages.subList(start, recentMessages.size())) {
            result.add(new LlmMessage(message.role().getValue(), message.content()));
        }

        result.add(new LlmMessage("user", buildContextBlock(conversation, characterName, latestUserMessage)));
        return result;
    }

    private String buildContextBlock(Conversation conversation, String characterName, String latestUserMessage) {
        Scene scene = conversation.scene();
        Relationship rel = conversation.relationship();
        CharacterRuntimeState runtime = conversation.characterState();
        CharacterHealth health = runtime != null && runtime.health() != null
                ? runtime.health()
                : new CharacterHealth(100, 100);

        String present = String.join(", ", scene.charactersPresent());
        String conflict = scene.currentConflict() != null ? scene.currentConflict() : "none";
        String memories = conversation.memories().stream()
                .limit(10)
                .map(StoryMemoryEntry::content)
                .map(m -> "- " + m)
                .collect(Collectors.joining("\n"));

        if (memories.isBlank()) {
            memories = "- (none yet)";
        }

        String runtimeLocation = runtime != null ? runtime.location() : scene.location();
        String status = runtime != null && runtime.status() != null ? runtime.status() : "none";
        String emotion = runtime != null && runtime.emotion() != null ? runtime.emotion() : "none";

        return """
                CURRENT SCENE
                Location: %s
                Time: %s
                Present: %s
                Situation: %s
                Conflict: %s

                CHARACTER RUNTIME STATE
                Health: %d/%d
                Location: %s
                Status: %s
                Emotion: %s

                RELATIONSHIP WITH USER
                Trust: %d/100, Respect: %d/100, Affection: %d/100, Familiarity: %d/100, Suspicion: %d/100
                (Use these as internal guidance for tone; do not recite numbers to the user.)

                IMPORTANT MEMORIES
                %s

                USER MESSAGE
                %s

                Return the structured JSON turn result now. Narrative goes in "response" only.
                """.formatted(
                scene.location(),
                scene.time(),
                present,
                scene.currentSituation(),
                conflict,
                health.current(),
                health.max(),
                runtimeLocation,
                status,
                emotion,
                rel.trust(),
                rel.respect(),
                rel.affection(),
                rel.familiarity(),
                rel.suspicion(),
                memories,
                latestUserMessage);
    }
}
