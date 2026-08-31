package com.aditya.roleplay.visual;

import com.aditya.roleplay.model.CharacterRuntimeState;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Scene;
import com.aditya.roleplay.model.World;
import com.aditya.roleplay.model.WorldLocation;
import com.aditya.roleplay.model.visual.VisualCharacterScenePresence;
import com.aditya.roleplay.model.visual.VisualSceneState;
import com.aditya.roleplay.service.PlayerPersonaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds transient visual scene state from authoritative runtime story state
 * and the most recent chat messages so generated images reflect the current moment.
 */
@ApplicationScoped
public class VisualScenePlannerService {

    private static final int RECENT_MESSAGE_LIMIT = 4;
    private static final int SNIPPET_LIMIT = 280;

    @Inject
    PlayerPersonaService personaService;

    public VisualSceneState plan(
            Conversation conversation,
            RoleplayCharacter focalCharacter,
            World world) {

        Scene scene = conversation.scene();
        CharacterRuntimeState characterState = conversation.characterState();
        String playerName = resolvePlayerName(conversation);
        List<Message> dialogueMessages = dialogueMessages(conversation.messages());
        String recentMoment = buildRecentMoment(dialogueMessages, playerName, focalCharacter.name());

        String location = scene != null ? scene.location() : "unknown";
        String locationDescription = resolveLocationDescription(world, location);
        String time = scene != null ? scene.time() : "unknown";
        String situation = mergeSituation(scene, recentMoment);

        List<VisualCharacterScenePresence> characters = new ArrayList<>();
        characters.add(buildCharacterPresence(
                focalCharacter, characterState, dialogueMessages, playerName));
        if (playerPresentInScene(scene)) {
            characters.add(buildPlayerPresence(playerName, dialogueMessages));
        }

        return new VisualSceneState(
                location,
                locationDescription,
                time,
                situation,
                characters,
                defaultCamera(scene, playerPresentInScene(scene)),
                defaultLighting(time),
                defaultAtmosphere(scene),
                recentMoment);
    }

    private VisualCharacterScenePresence buildCharacterPresence(
            RoleplayCharacter character,
            CharacterRuntimeState state,
            List<Message> dialogueMessages,
            String playerName) {

        Optional<Message> lastAssistant = lastMessageWithRole(dialogueMessages, Role.ASSISTANT);
        Optional<Message> lastUser = lastMessageWithRole(dialogueMessages, Role.USER);

        String expression = state != null && state.emotion() != null && !state.emotion().isBlank()
                ? state.emotion()
                : "expression matching the current dialogue moment";
        String action = deriveFocalAction(state, lastUser, lastAssistant, playerName);
        String pose = derivePose(lastUser, lastAssistant, action);

        return new VisualCharacterScenePresence(
                character.id(),
                character.name(),
                pose,
                expression,
                action,
                null);
    }

    private VisualCharacterScenePresence buildPlayerPresence(String playerName, List<Message> dialogueMessages) {
        Optional<Message> lastUser = lastMessageWithRole(dialogueMessages, Role.USER);
        String action = lastUser
                .map(message -> "currently: " + truncate(message.content(), SNIPPET_LIMIT))
                .orElse("present and interacting nearby");

        return new VisualCharacterScenePresence(
                "player",
                playerName,
                "engaged in the scene",
                "attentive",
                action,
                null);
    }

    private static String deriveFocalAction(
            CharacterRuntimeState state,
            Optional<Message> lastUser,
            Optional<Message> lastAssistant,
            String playerName) {

        StringBuilder action = new StringBuilder();
        lastUser.ifPresent(message -> action.append("Responding to ")
                .append(playerName)
                .append(" who ")
                .append(truncate(message.content(), SNIPPET_LIMIT))
                .append(". "));
        lastAssistant.ifPresent(message -> action.append("Currently depicted as: ")
                .append(truncate(message.content(), SNIPPET_LIMIT)));

        if (action.length() > 0) {
            return action.toString().trim();
        }
        if (state != null && state.status() != null && !state.status().isBlank()) {
            return state.status();
        }
        return "present in scene";
    }

    private static String derivePose(
            Optional<Message> lastUser,
            Optional<Message> lastAssistant,
            String action) {

        String combined = (lastUser.map(Message::content).orElse("") + " "
                + lastAssistant.map(Message::content).orElse("") + " "
                + action).toLowerCase();

        if (containsAny(combined, "hold", "hand", "embrace", "hug", "touch", "give", "offer", "bread", "gift")) {
            return "close interaction, hands visible, intimate framing";
        }
        if (containsAny(combined, "sit", "seated", "bench", "ground", "lean")) {
            return "seated or leaning posture";
        }
        if (containsAny(combined, "walk", "approach", "step", "move closer")) {
            return "mid-movement, approaching or shifting position";
        }
        if (containsAny(combined, "fight", "draw", "weapon", "attack", "defend")) {
            return "dynamic action pose";
        }
        return "natural stance suited to the current moment";
    }

    private static String buildRecentMoment(
            List<Message> dialogueMessages,
            String playerName,
            String characterName) {

        if (dialogueMessages.isEmpty()) {
            return null;
        }

        int start = Math.max(0, dialogueMessages.size() - RECENT_MESSAGE_LIMIT);
        StringBuilder moment = new StringBuilder();
        for (Message message : dialogueMessages.subList(start, dialogueMessages.size())) {
            String speaker = message.role() == Role.USER ? playerName : characterName;
            moment.append(speaker)
                    .append(": ")
                    .append(truncate(message.content(), SNIPPET_LIMIT))
                    .append('\n');
        }
        return moment.toString().trim();
    }

    private static String mergeSituation(Scene scene, String recentMoment) {
        String base = scene != null ? scene.currentSituation() : null;
        if (recentMoment == null || recentMoment.isBlank()) {
            return base;
        }
        if (base == null || base.isBlank()) {
            return "Current moment from chat:\n" + recentMoment;
        }
        return base + "\n\nCurrent moment from chat:\n" + recentMoment;
    }

    private String resolvePlayerName(Conversation conversation) {
        if (conversation.playerPersonaId() == null || conversation.playerPersonaId().isBlank()) {
            return "the player";
        }
        try {
            PlayerPersona persona = personaService.requirePersona(conversation.playerPersonaId());
            if (persona.name() != null && !persona.name().isBlank()) {
                return persona.name();
            }
        } catch (RuntimeException ignored) {
            // Fall back to generic label when persona is missing.
        }
        return "the player";
    }

    private static List<Message> dialogueMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream()
                .filter(message -> !message.isSceneImage())
                .filter(message -> message.content() != null && !message.content().isBlank())
                .toList();
    }

    private static Optional<Message> lastMessageWithRole(List<Message> messages, Role role) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message.role() == role) {
                return Optional.of(message);
            }
        }
        return Optional.empty();
    }

    private static boolean playerPresentInScene(Scene scene) {
        if (scene == null || scene.charactersPresent() == null) {
            return true;
        }
        return scene.charactersPresent().stream()
                .anyMatch(id -> "user".equalsIgnoreCase(id) || "player".equalsIgnoreCase(id));
    }

    private String resolveLocationDescription(World world, String locationId) {
        if (world == null || world.locations() == null) {
            return locationId;
        }
        return world.locations().stream()
                .filter(loc -> locationId.equals(loc.id()))
                .map(WorldLocation::description)
                .findFirst()
                .orElse(locationId);
    }

    private String defaultCamera(Scene scene, boolean playerPresent) {
        if (playerPresent) {
            return "medium-wide two-character shot, eye level";
        }
        if (scene != null && scene.charactersPresent() != null && scene.charactersPresent().size() > 2) {
            return "medium-wide shot, eye level";
        }
        return "medium shot, eye level";
    }

    private String defaultLighting(String time) {
        if (time == null) {
            return "soft ambient lighting";
        }
        String normalized = time.toLowerCase();
        if (normalized.contains("night") || normalized.contains("evening")) {
            return "low warm interior lighting with soft shadows";
        }
        if (normalized.contains("morning") || normalized.contains("dawn")) {
            return "cool morning light with gentle highlights";
        }
        return "balanced cinematic lighting";
    }

    private String defaultAtmosphere(Scene scene) {
        if (scene != null && scene.currentConflict() != null && !scene.currentConflict().isBlank()) {
            return "tense atmosphere, " + scene.currentConflict();
        }
        return "immersive fantasy atmosphere";
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength - 3) + "...";
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
