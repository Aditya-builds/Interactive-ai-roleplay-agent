package com.aditya.roleplay.service;

import com.aditya.roleplay.llm.LlmMessage;
import com.aditya.roleplay.llm.LlmRequest;
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

    public LlmRequest build(
            RoleplayCharacter character,
            World world,
            Conversation conversation,
            String latestUserMessage) {

        String systemPrompt = buildSystemPrompt(character, world);
        List<Message> recentMessages = selectRecentMessages(conversation.messages());
        List<LlmMessage> chatMessages = buildChatMessages(recentMessages, character.name(), latestUserMessage, conversation);

        return new LlmRequest(systemPrompt, chatMessages, temperature, maxTokens);
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
                - Stay in character at all times.
                - You control ONLY %s, NPCs, and the environment.
                - NEVER control the user's character: do not describe what the user does, thinks, feels, or says unless they explicitly wrote it.
                - Do not write the user's dialogue or internal monologue.
                - Maintain continuity with the scene, memories, and recent conversation.
                - Do not invent contradictory facts about the character or world.
                - Do not force romantic progression; let relationships develop naturally.
                - Respond in present tense, narrative prose suitable for a roleplay chat.
                - Keep responses concise: 1-4 short paragraphs unless the moment demands more.
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
                character.name());
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

        // Include up to last 6 pairs as native chat roles (excluding the latest user message)
        List<Message> historyForChat = recentMessages;
        if (!historyForChat.isEmpty()) {
            Message last = historyForChat.get(historyForChat.size() - 1);
            if (last.role() == Role.USER && last.content().equals(latestUserMessage)) {
                historyForChat = historyForChat.subList(0, historyForChat.size() - 1);
            }
        }

        int start = Math.max(0, historyForChat.size() - 12);
        for (Message message : historyForChat.subList(start, historyForChat.size())) {
            result.add(new LlmMessage(message.role().getValue(), message.content()));
        }

        result.add(new LlmMessage("user", buildContextBlock(conversation, characterName, latestUserMessage)));
        return result;
    }

    private String buildContextBlock(Conversation conversation, String characterName, String latestUserMessage) {
        Scene scene = conversation.scene();
        Relationship rel = conversation.relationship();

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

        return """
                CURRENT SCENE
                Location: %s
                Time: %s
                Present: %s
                Situation: %s
                Conflict: %s

                RELATIONSHIP WITH USER
                Trust: %d/100, Respect: %d/100, Affection: %d/100, Familiarity: %d/100, Suspicion: %d/100
                (Use these as internal guidance for tone; do not recite numbers to the user.)

                IMPORTANT MEMORIES
                %s

                USER MESSAGE
                %s

                Respond as %s only.
                """.formatted(
                scene.location(),
                scene.time(),
                present,
                scene.currentSituation(),
                conflict,
                rel.trust(),
                rel.respect(),
                rel.affection(),
                rel.familiarity(),
                rel.suspicion(),
                memories,
                latestUserMessage,
                characterName);
    }
}
