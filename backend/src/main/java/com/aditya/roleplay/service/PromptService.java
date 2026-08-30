package com.aditya.roleplay.service;

import com.aditya.roleplay.llm.LlmMessage;
import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
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

        String systemPrompt = buildSystemPrompt(character, world, allowedRelationshipTargets);
        List<Message> recentMessages = selectRecentMessages(conversation.messages());
        List<LlmMessage> chatMessages = buildChatMessages(recentMessages, latestUserMessage, conversation, allowedRelationshipTargets);

        return new LlmRequest(systemPrompt, chatMessages, temperature, maxTokens, jsonMode);
    }

    private String buildSystemPrompt(RoleplayCharacter character, World world, Set<String> allowedRelationshipTargets) {
        String personality = String.join(", ", character.personality());
        String values = character.values().stream()
                .map(v -> "- " + v)
                .collect(Collectors.joining("\n"));
        String rules = world.rules().stream()
                .map(r -> "- " + r)
                .collect(Collectors.joining("\n"));
        String relationshipTargets = allowedRelationshipTargets.stream()
                .sorted()
                .collect(Collectors.joining(", "));

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
                5. Maintain continuity with the scene, memories, events, and recent conversation.
                6. Do not invent contradictory facts about the character or world.
                7. Do not force romantic progression; let relationships develop naturally.
                8. Propose stateChanges every turn when the interaction affects mood, relationship, health, or scene dynamics. Narrative and stateChanges must agree.
                9. Keep narrative responses concise: 1-4 short paragraphs unless the moment demands more.
                10. Return ONLY valid JSON matching the schema below. No markdown, no extra text.

                STATE CHANGE REQUIREMENTS (MANDATORY WHEN APPLICABLE)
                - If the user shows care, concern, trust, or physical comfort (e.g. asking if they are alright, holding a hand): propose RELATIONSHIP trust and/or affection INCREASE (1-%d) AND EMOTION if %s's mood shifts.
                - If your narrative describes %s's expression or mood changing (softens, gratitude, anger, worry, warmth, irritation, weariness): you MUST propose EMOTION with a specific value (grateful, warm, weary, guarded, irritated, focused, touched — not generic "calm" unless truly neutral).
                - If %s mentions being tired, injured, or unwell: propose STATUS (exhausted or injured) and EMOTION (weary or pained) as appropriate.
                - If the active scene dynamic changes (e.g. from reviewing reports to a personal moment, from talking to sparring): propose SCENE currentSituation SET to a short new description.
                - Do not leave emotion, status, and situation unchanged when your narrative clearly describes a different mood or moment.

                STRUCTURED OUTPUT SCHEMA
                Return a single JSON object with these fields:

                {
                  "response": "Narrative reply the player sees, written in character.",
                  "stateChanges": [
                    {
                      "type": "RELATIONSHIP | SCENE | HEALTH | LOCATION | STATUS | EMOTION",
                      "targetId": "relationship partner id (%s) OR 'scene' OR '%s' for character state",
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
                - SCENE targetId must be "scene". Fields: location (NPC location), userLocation (user location when split), time, currentSituation, currentConflict, charactersPresent, addCharacter, removeCharacter. Use SET only. Location slugs: guild_hall, forest, training_ground, etc. charactersPresent lists who is with the NPC right now, e.g. ["%s","user"] when together, or ["%s"] when user left. SET currentSituation to "null" when an activity ends.
                - HEALTH targetId must be %s. Field: current. Use INCREASE/DECREASE/SET for damage or healing.
                - LOCATION targetId: use "%s" or "scene" when the NPC moves (updates NPC + scene location; user follows if still present). Use "user" when ONLY the user moves alone (does NOT move the NPC). Field: location. SET only.
                - STATUS targetId must be %s. Field: status. SET only (injured, exhausted, or null). When you narrate injury or damage to %s, propose STATUS (injured) alongside HEALTH changes.
                - EMOTION targetId must be %s. Field: emotion. SET only. Use specific emotions: grateful, warm, weary, guarded, irritated, focused, touched, angry, calm. Avoid defaulting to calm when the narrative shows a clear emotional shift.
                - When the user leaves alone: LOCATION user + removeCharacter user (or SCENE userLocation), do NOT change NPC location.
                - When location or participants change, clear stale situation by SET currentSituation to "null" unless setting a new one in the same turn.
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
                maxRelationshipDelta,
                character.name(),
                character.name(),
                character.name(),
                relationshipTargets,
                character.id(),
                character.name(),
                relationshipTargets,
                maxRelationshipDelta,
                character.id(),
                character.id(),
                character.id(),
                character.id(),
                character.id(),
                character.name(),
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
            String latestUserMessage,
            Conversation conversation,
            Set<String> allowedRelationshipTargets) {

        List<LlmMessage> result = new ArrayList<>();

        for (Message message : recentMessages) {
            if (message.role() == Role.USER && message.content().equals(latestUserMessage)) {
                continue;
            }
            result.add(new LlmMessage(message.role().getValue(), message.content()));
        }

        result.add(new LlmMessage(
                "user",
                promptContextService.buildTurnContext(conversation, latestUserMessage, allowedRelationshipTargets)));
        return result;
    }
}
