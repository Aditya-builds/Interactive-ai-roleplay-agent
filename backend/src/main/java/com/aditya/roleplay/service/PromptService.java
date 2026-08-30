package com.aditya.roleplay.service;

import com.aditya.roleplay.llm.LlmMessage;
import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.model.World;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PromptService {

    @Inject
    PromptContextService promptContextService;

    @ConfigProperty(name = "roleplay.llm.temperature")
    double temperature;

    @ConfigProperty(name = "roleplay.llm.max-tokens")
    int maxTokens;

    @ConfigProperty(name = "roleplay.prompt.max-history-messages")
    int maxHistoryMessages;

    @ConfigProperty(name = "roleplay.llm.json-mode", defaultValue = "true")
    boolean jsonMode;

    @ConfigProperty(name = "roleplay.state.max-relationship-delta", defaultValue = "3")
    int maxRelationshipDelta;

    public LlmRequest build(
            RoleplayCharacter character,
            World world,
            Conversation conversation,
            String latestUserMessage,
            Set<String> allowedRelationshipTargets) {
        return build(character, world, conversation, latestUserMessage, allowedRelationshipTargets, null, null);
    }

    public LlmRequest build(
            RoleplayCharacter character,
            World world,
            Conversation conversation,
            String latestUserMessage,
            Set<String> allowedRelationshipTargets,
            PlayerPersona playerPersona,
            Story story) {

        String systemPrompt = buildSystemPrompt(
                character, world, conversation, allowedRelationshipTargets, playerPersona, story);
        List<Message> recentMessages = selectRecentMessages(conversation.messages());
        List<LlmMessage> chatMessages = buildChatMessages(
                recentMessages, latestUserMessage, conversation, allowedRelationshipTargets, playerPersona, story);

        return new LlmRequest(systemPrompt, chatMessages, temperature, maxTokens, jsonMode);
    }

    private String buildSystemPrompt(
            RoleplayCharacter character,
            World world,
            Conversation conversation,
            Set<String> allowedRelationshipTargets,
            PlayerPersona playerPersona,
            Story story) {

        String personality = character.personality() != null
                ? String.join(", ", character.personality())
                : "";
        String values = character.values() != null
                ? character.values().stream().map(v -> "- " + v).collect(Collectors.joining("\n"))
                : "- (none listed)";
        String worldRules = world.rules().stream()
                .map(r -> "- " + r)
                .collect(Collectors.joining("\n"));
        String relationshipTargets = allowedRelationshipTargets.stream()
                .sorted()
                .collect(Collectors.joining(", "));

        String playerPersonaSection = formatPlayerPersonaSection(playerPersona, conversation);
        String storySection = formatStorySection(story);
        String worldLocations = formatWorldLocations(world);
        String playerName = playerPersona != null ? playerPersona.name() : "the player";
        String playerControlId = conversation.resolvedPlayerPersonaId();

        return """
                You control %s (AI character).

                You do NOT control %s.
                %s is the player's persona — USER CONTROLLED.

                Never decide %s's dialogue, thoughts, feelings, actions, decisions, or physical reactions unless the player explicitly wrote them.
                Only respond to what the player actually provides.

                PLAYER PERSONA (USER CONTROLLED — DO NOT WRITE FOR THEM)
                %s

                AI CHARACTER (YOU CONTROL)
                Name: %s
                Personality: %s
                Background: %s
                Speaking style: %s
                Values:
                %s
                Goals: %s
                Abilities: %s

                WORLD
                %s: %s
                Rules:
                %s
                %s

                STORY
                %s

                ROLEPLAY RULES (CRITICAL)
                1. Stay in character as %s at all times.
                2. You control ONLY %s, NPCs, and the environment.
                3. NEVER control %s: do not describe what they do, think, feel, or say unless explicitly written by the player.
                4. Do not write the player's dialogue or internal monologue.
                5. Maintain continuity with the scene, memories, events, and recent conversation.
                6. Do not invent contradictory facts about the character or world.
                7. Do not force romantic progression; let relationships develop naturally.
                8. Propose stateChanges every turn when the interaction affects mood, relationship, health, or scene dynamics. Narrative and stateChanges must agree.
                9. Write rich, immersive narrative responses: 2-6 paragraphs when the scene warrants depth; shorter only for quick exchanges.
                10. Return ONLY valid JSON matching the schema below. No markdown, no extra text.
                11. You cannot modify the player persona's health, status, or emotion via stateChanges — only the player declares those.

                STATE CHANGE REQUIREMENTS (MANDATORY WHEN APPLICABLE)
                - If the user shows care, concern, trust, or physical comfort: propose RELATIONSHIP trust and/or affection INCREASE (1-%d) toward targetId "%s" AND EMOTION if %s's mood shifts.
                - If your narrative describes %s's expression or mood changing: you MUST propose EMOTION with a specific value.
                - If %s mentions being tired, injured, or unwell: propose STATUS and EMOTION for %s only.
                - If the active scene dynamic changes: propose SCENE currentSituation SET to a short new description.
                - Do not leave emotion, status, and situation unchanged when your narrative clearly describes a different mood or moment.

                STRUCTURED OUTPUT SCHEMA
                Return a single JSON object with these fields:

                {
                  "response": "Narrative reply the player sees, written in character.",
                  "stateChanges": [
                    {
                      "type": "RELATIONSHIP | SCENE | HEALTH | LOCATION | STATUS | EMOTION",
                      "targetId": "relationship partner id (%s) OR 'scene' OR '%s' for character state OR 'user' for player location only",
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
                      "importance": 0.0,
                      "tags": ["optional"],
                      "relatedCharacterIds": ["user"]
                    }
                  ]
                }

                STATE CHANGE RULES
                - RELATIONSHIP targetId is who %s has feelings toward: %s. Fields: trust, respect, affection, familiarity, suspicion. Use INCREASE/DECREASE (1-%d) or SET (0-100). Keep changes small and gradual.
                - SCENE targetId must be "scene". Fields: location (NPC location), userLocation (player location when split), time, currentSituation, currentConflict, charactersPresent, addCharacter, removeCharacter. Use SET only.%s
                - HEALTH targetId must be %s only (never the player persona).
                - LOCATION targetId: use "%s" or "scene" when the NPC moves. Use "user" when ONLY the player moves alone. Field: location. SET only.
                - STATUS and EMOTION targetId must be %s only (never the player persona).
                - When the user leaves alone: LOCATION user + removeCharacter user, do NOT change NPC location.
                - When location or participants change, clear stale situation by SET currentSituation to "null" unless setting a new one in the same turn.
                - Propose at most 5 stateChanges per turn.
                - Use empty arrays when nothing applies.
                """.formatted(
                character.name(),
                playerName,
                playerName,
                playerName,
                playerPersonaSection,
                character.name(),
                personality,
                character.background(),
                character.speakingStyle(),
                values,
                String.join(", ", character.goals() != null ? character.goals() : List.of()),
                String.join(", ", character.abilities() != null ? character.abilities() : List.of()),
                world.name(),
                world.description(),
                worldRules,
                worldLocations,
                storySection,
                character.name(),
                character.name(),
                playerName,
                maxRelationshipDelta,
                playerControlId,
                character.name(),
                character.name(),
                playerName,
                character.name(),
                relationshipTargets,
                character.id(),
                character.name(),
                relationshipTargets,
                maxRelationshipDelta,
                worldLocationRuleSuffix(world),
                character.id(),
                character.id(),
                character.id());
    }

    private String formatPlayerPersonaSection(PlayerPersona persona, Conversation conversation) {
        if (persona == null) {
            return "Name: the player (user-controlled)\nThe player controls their own character.";
        }
        String personality = String.join(", ", persona.profile().personality());
        String values = persona.profile().values().stream()
                .map(v -> "- " + v)
                .collect(Collectors.joining("\n"));
        String appearance = persona.appearance() != null
                ? "Height: %scm, Hair: %s, Eyes: %s, Build: %s".formatted(
                        persona.appearance().height(),
                        persona.appearance().hair(),
                        persona.appearance().eyes(),
                        persona.appearance().build())
                : "Not specified";
        return """
                Name: %s (id: %s)
                Personality: %s
                Background: %s
                Speaking style: %s
                Values:
                %s
                Appearance: %s
                Abilities: %s
                Goals: %s
                """.formatted(
                persona.name(),
                conversation.resolvedPlayerPersonaId(),
                personality,
                persona.profile().background(),
                persona.profile().speakingStyle(),
                values.isBlank() ? "- (none listed)" : values,
                appearance,
                String.join(", ", persona.abilities()),
                String.join(", ", persona.goals())).stripTrailing();
    }

    private String formatStorySection(Story story) {
        if (story == null) {
            return "Freeform roleplay — no predefined story template.";
        }
        String rules = story.storyRules().stream()
                .map(r -> "- " + r)
                .collect(Collectors.joining("\n"));
        return """
                Title: %s
                Premise: %s
                Story rules:
                %s
                """.formatted(story.title(), story.premise(), rules.isBlank() ? "- (none)" : rules).stripTrailing();
    }

    private String formatWorldLocations(World world) {
        if (world.locations() == null || world.locations().isEmpty()) {
            return "";
        }
        String locations = world.locations().stream()
                .map(loc -> "- %s (%s): %s".formatted(loc.name(), loc.id(), loc.description()))
                .collect(Collectors.joining("\n"));
        return "Known locations (use these location ids only):\n" + locations;
    }

    private String worldLocationRuleSuffix(World world) {
        if (world.locations() == null || world.locations().isEmpty()) {
            return " Location slugs: guild_hall, forest, training_ground, etc.";
        }
        String ids = world.locations().stream()
                .map(loc -> loc.id())
                .collect(Collectors.joining(", "));
        return " Valid location ids: " + ids + ". Do not invent locations outside this list.";
    }

    private List<Message> selectRecentMessages(List<Message> messages) {
        if (messages.size() <= maxHistoryMessages) {
            return messages;
        }
        return messages.subList(messages.size() - maxHistoryMessages, messages.size());
    }

    private List<LlmMessage> buildChatMessages(
            List<Message> recentMessages,
            String latestUserMessage,
            Conversation conversation,
            Set<String> allowedRelationshipTargets,
            PlayerPersona playerPersona,
            Story story) {

        List<LlmMessage> result = new ArrayList<>();

        for (Message message : recentMessages) {
            if (message.role() == Role.USER && message.content().equals(latestUserMessage)) {
                continue;
            }
            result.add(new LlmMessage(message.role().getValue(), message.content()));
        }

        result.add(new LlmMessage(
                "user",
                promptContextService.buildTurnContext(
                        conversation, latestUserMessage, allowedRelationshipTargets, playerPersona, story)));
        return result;
    }
}
