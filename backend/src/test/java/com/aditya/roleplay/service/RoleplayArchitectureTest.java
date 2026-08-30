package com.aditya.roleplay.service;

import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.CreateConversationRequest;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Scene;
import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.model.World;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import com.aditya.roleplay.storage.JsonStorageService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class RoleplayArchitectureTest {

    @Inject
    PlayerPersonaService personaService;

    @Inject
    StoryService storyService;

    @Inject
    CharacterService characterService;

    @Inject
    ConversationService conversationService;

    @Inject
    PromptService promptService;

    @Inject
    StateChangeValidator stateChangeValidator;

    @Inject
    PromptContextService promptContextService;

    @Inject
    JsonStorageService storage;

    @Test
    void personaLoadsCorrectly() {
        PlayerPersona persona = personaService.requirePersona("aurora");
        assertEquals("aurora", persona.id());
        assertEquals("Aurora", persona.name());
        assertEquals(178, persona.appearance().height());
        assertTrue(persona.profile().personality().contains("calm"));
    }

    @Test
    void storyLoadsCorrectly() {
        Story story = storyService.requireStory("runa_ashbitten");
        assertEquals("Ashbitten", story.title());
        assertEquals("shattered_world", story.worldId());
        assertEquals("east_market_arch", story.startingLocation());
        assertFalse(story.storyRules().isEmpty());
    }

    @Test
    void storyConversationInitializesFromOpeningScene() {
        Conversation conversation = conversationService.create(new CreateConversationRequest(
                null,
                "aurora",
                "runa_ashbitten",
                "runa",
                List.of("runa")));

        assertEquals("aurora", conversation.playerPersonaId());
        assertEquals("runa_ashbitten", conversation.storyId());
        assertEquals("shattered_world", conversation.worldId());
        assertEquals("runa", conversation.characterId());
        assertEquals(List.of("runa"), conversation.activeCharacterIds());
        assertEquals("east_market_arch", conversation.scene().location());
        assertFalse(conversation.messages().isEmpty());
        assertTrue(conversation.messages().get(0).content().contains("Runa Ashlock"));
    }

    @Test
    void personaAndStoryReachPromptService() {
        RoleplayCharacter runa = characterService.requireCharacter("runa");
        World world = characterService.requireWorld("shattered_world");
        PlayerPersona persona = personaService.requirePersona("aurora");
        Story story = storyService.requireStory("runa_ashbitten");

        Conversation conversation = conversationService.create(new CreateConversationRequest(
                null, "aurora", "runa_ashbitten", "runa", List.of("runa")));

        LlmRequest request = promptService.build(
                runa, world, conversation, "Aurora approaches the board.", Set.of("aurora", "user"), persona, story);

        String system = request.systemPrompt();
        assertTrue(system.contains("PLAYER PERSONA"));
        assertTrue(system.contains("Aurora"));
        assertTrue(system.contains("You do NOT control Aurora"));
        assertTrue(system.contains("AI CHARACTER"));
        assertTrue(system.contains("Runa Ashlock"));
        assertTrue(system.contains("Ashbitten"));
        assertTrue(system.contains("east_market_arch"));
        assertTrue(system.contains("Never decide Aurora's dialogue, thoughts, feelings, actions, decisions, or physical reactions"));
    }

    @Test
    void playerAgencyRuleInPrompt() {
        RoleplayCharacter runa = characterService.requireCharacter("runa");
        World world = characterService.requireWorld("shattered_world");
        PlayerPersona persona = personaService.requirePersona("aurora");

        Conversation conversation = new Conversation(
                UUID.randomUUID().toString(),
                "runa",
                "shattered_world",
                Instant.now(),
                Instant.now(),
                null,
                new Scene("east_market_arch", "east_market_arch", "afternoon", List.of("runa", "user"), "Opening.", null),
                List.of(new Relationship("runa", "aurora", 10, 20, 0, 0, 35)),
                List.of(),
                List.of(),
                List.of(),
                "aurora",
                "runa_ashbitten",
                List.of("runa"),
                null);

        LlmRequest request = promptService.build(
                runa, world, conversation, "Hello.", Set.of("aurora"), persona,
                storyService.requireStory("runa_ashbitten"));

        assertTrue(request.systemPrompt().contains("USER CONTROLLED"));
        assertTrue(request.systemPrompt().contains("Never decide Aurora's dialogue, thoughts, feelings, actions, decisions, or physical reactions"));
    }

    @Test
    void directionalRelationshipsAreDistinct() {
        Relationship runaToAurora = new Relationship("runa", "aurora", 10, 20, 0, 0, 35);
        Relationship auroraToRuna = new Relationship("aurora", "runa", 50, 40, 5, 10, 10);

        Conversation conversation = new Conversation(
                UUID.randomUUID().toString(),
                "runa",
                "shattered_world",
                Instant.now(),
                Instant.now(),
                null,
                new Scene("east_market_arch", "east_market_arch", "afternoon", List.of("runa", "user"), null, null),
                List.of(runaToAurora, auroraToRuna),
                List.of(),
                List.of(),
                List.of(new Message(UUID.randomUUID().toString(), Role.USER, "test", Instant.now())),
                "aurora",
                "runa_ashbitten",
                List.of("runa"),
                null);

        String context = promptContextService.buildTurnContext(
                conversation, "test", Set.of("aurora"), personaService.requirePersona("aurora"), null);

        assertTrue(context.contains("runa → aurora: trust 10"));
        assertFalse(context.contains("aurora → runa"));
    }

    @Test
    void invalidLocationRejectedForWorldWithCatalog() {
        World world = characterService.requireWorld("shattered_world");
        StateChange invalid = new StateChange(
                StateChangeType.LOCATION,
                "runa",
                "location",
                StateChangeOperation.SET,
                "made_up_place");

        StateChangeValidator.ValidationResult result = stateChangeValidator.validate(
                invalid, "runa", Set.of("aurora"), null, world);

        assertFalse(result.valid());
        assertTrue(result.reason().contains("unknown location"));
    }

    @Test
    void validLocationAcceptedForWorldWithCatalog() {
        World world = characterService.requireWorld("shattered_world");
        StateChange valid = new StateChange(
                StateChangeType.LOCATION,
                "runa",
                "location",
                StateChangeOperation.SET,
                "forest");

        StateChangeValidator.ValidationResult result = stateChangeValidator.validate(
                valid, "runa", Set.of("aurora"), null, world);

        assertTrue(result.valid());
    }

    @Test
    void conversationPersistenceIncludesStoryFields() {
        Conversation created = conversationService.create(new CreateConversationRequest(
                null, "aurora", "runa_ashbitten", "runa", List.of("runa")));

        Conversation loaded = storage.loadConversation(created.id()).orElseThrow();
        assertEquals("aurora", loaded.playerPersonaId());
        assertEquals("runa_ashbitten", loaded.storyId());
        assertEquals("shattered_world", loaded.worldId());
        assertEquals(List.of("runa"), loaded.activeCharacterIds());
        assertNotNull(loaded.scene());
        assertNotNull(loaded.characterState());
        assertFalse(loaded.relationships().isEmpty());
        assertFalse(loaded.messages().isEmpty());

        conversationService.deleteConversation(created.id());
    }

    @Test
    void legacyCharacterConversationStillWorks() {
        Conversation conversation = conversationService.create(new CreateConversationRequest(
                "aurora", null, null, null, null));
        assertEquals("aurora", conversation.characterId());
        assertEquals("fantasy", conversation.worldId());
        assertNotNull(conversation.scene());

        conversationService.deleteConversation(conversation.id());
    }
}
