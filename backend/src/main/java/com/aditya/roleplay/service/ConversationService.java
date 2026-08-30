package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.CharacterRuntimeState;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.ConversationSummary;
import com.aditya.roleplay.model.CreateConversationRequest;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.model.World;
import com.aditya.roleplay.storage.JsonStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ConversationService {

    @Inject
    JsonStorageService storage;

    @Inject
    CharacterService characterService;

    @Inject
    PlayerPersonaService personaService;

    @Inject
    StoryService storyService;

    @Inject
    StoryStateService storyStateService;

    @Inject
    RelationshipService relationshipService;

    public Conversation create(CreateConversationRequest request) {
        if (request.storyId() != null && !request.storyId().isBlank()) {
            return createFromStory(request);
        }
        if (request.characterId() == null || request.characterId().isBlank()) {
            throw new RoleplayException("characterId or storyId is required", "INVALID_REQUEST", 400);
        }
        return createLegacyConversation(request.characterId());
    }

    public Conversation createConversation(String characterId) {
        return createLegacyConversation(characterId);
    }

    private Conversation createLegacyConversation(String characterId) {
        RoleplayCharacter character = characterService.requireCharacter(characterId);
        characterService.requireWorld(character.worldId());

        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();

        Conversation conversation = new Conversation(
                id,
                character.id(),
                character.worldId(),
                now,
                now,
                storyStateService.createInitialCharacterState(character),
                storyStateService.createInitialScene(character),
                relationshipService.createInitialRelationships(character),
                List.of(),
                characterService.seedMemoriesForCharacter(character, now),
                characterService.initialMessagesForCharacter(character, now),
                null,
                null,
                List.of(character.id()),
                null);

        return storage.saveConversation(conversation);
    }

    private Conversation createFromStory(CreateConversationRequest request) {
        Story story = storyService.requireStory(request.storyId());
        World world = characterService.requireWorld(story.worldId());

        String playerPersonaId = request.playerPersonaId();
        if (playerPersonaId == null || playerPersonaId.isBlank()) {
            throw new RoleplayException("playerPersonaId is required for story conversations", "INVALID_REQUEST", 400);
        }

        PlayerPersona persona = personaService.requirePersona(playerPersonaId);
        if (persona.worldId() != null && !persona.worldId().isBlank()
                && !story.worldId().equals(persona.worldId())) {
            throw new RoleplayException(
                    "Persona world does not match story world",
                    "INVALID_REQUEST",
                    400);
        }

        String focalCharacterId = resolveFocalCharacterId(request, story);
        RoleplayCharacter character = characterService.requireCharacter(focalCharacterId);
        if (!story.worldId().equals(character.worldId())) {
            throw new RoleplayException(
                    "Character world does not match story world",
                    "INVALID_REQUEST",
                    400);
        }

        List<String> activeCharacterIds = resolveActiveCharacterIds(request, story, focalCharacterId);
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();

        Conversation conversation = new Conversation(
                id,
                focalCharacterId,
                story.worldId(),
                now,
                now,
                storyStateService.createInitialCharacterStateForStory(story, character),
                storyStateService.createInitialSceneFromStory(story, character, playerPersonaId),
                relationshipService.createInitialRelationshipsFromStory(story, character, playerPersonaId),
                List.of(),
                characterService.seedMemoriesForCharacter(character, now),
                initialMessagesForStory(story, now),
                playerPersonaId,
                story.id(),
                activeCharacterIds,
                storyStateService.createInitialPlayerPersonaState(story, persona));

        if (story.startingLocation() != null && !world.isValidLocation(story.startingLocation())) {
            throw new RoleplayException(
                    "Story starting location is not defined in world: " + story.startingLocation(),
                    "INVALID_STORY",
                    400);
        }

        return storage.saveConversation(conversation);
    }

    private String resolveFocalCharacterId(CreateConversationRequest request, Story story) {
        if (request.focalCharacterId() != null && !request.focalCharacterId().isBlank()) {
            return request.focalCharacterId();
        }
        if (!story.startingCharacters().isEmpty()) {
            return story.startingCharacters().get(0);
        }
        throw new RoleplayException("focalCharacterId is required", "INVALID_REQUEST", 400);
    }

    private List<String> resolveActiveCharacterIds(
            CreateConversationRequest request,
            Story story,
            String focalCharacterId) {
        if (request.activeCharacterIds() != null && !request.activeCharacterIds().isEmpty()) {
            return List.copyOf(request.activeCharacterIds());
        }
        if (!story.startingCharacters().isEmpty()) {
            return List.copyOf(story.startingCharacters());
        }
        return List.of(focalCharacterId);
    }

    private List<Message> initialMessagesForStory(Story story, Instant now) {
        if (story.openingNarrative() == null || story.openingNarrative().isBlank()) {
            return List.of();
        }
        return List.of(new Message(
                UUID.randomUUID().toString(),
                Role.ASSISTANT,
                story.openingNarrative().trim(),
                now));
    }

    public Conversation getConversation(String id) {
        Conversation conversation = storage.loadConversation(id)
                .orElseThrow(() -> new RoleplayException("Conversation not found: " + id, "CONVERSATION_NOT_FOUND", 404));
        return migrateIfNeeded(conversation);
    }

    public Conversation save(Conversation conversation) {
        return storage.saveConversation(conversation);
    }

    public List<ConversationSummary> listConversations() {
        return storage.listConversations();
    }

    public void deleteConversation(String id) {
        storage.loadConversation(id)
                .orElseThrow(() -> new RoleplayException("Conversation not found: " + id, "CONVERSATION_NOT_FOUND", 404));
        storage.deleteConversation(id);
    }

    private Conversation migrateIfNeeded(Conversation conversation) {
        boolean needsSave = false;
        Conversation updated = conversation;

        if (conversation.characterState() == null) {
            RoleplayCharacter character = characterService.requireCharacter(conversation.characterId());
            updated = updated.withCharacterState(storyStateService.createInitialCharacterState(character));
            needsSave = true;
        }
        if (conversation.events() == null) {
            updated = updated.withEvents(List.of());
            needsSave = true;
        }
        if (conversation.relationships() == null || conversation.relationships().isEmpty()) {
            RoleplayCharacter character = characterService.requireCharacter(conversation.characterId());
            updated = updated.withRelationships(relationshipService.createInitialRelationships(character));
            needsSave = true;
        } else {
            List<Relationship> migratedRelationships = migrateRelationships(updated);
            if (!migratedRelationships.equals(updated.relationships())) {
                updated = updated.withRelationships(migratedRelationships);
                needsSave = true;
            }
        }

        if (updated.activeCharacterIds().isEmpty() && updated.characterId() != null) {
            updated = new Conversation(
                    updated.id(),
                    updated.characterId(),
                    updated.worldId(),
                    updated.createdAt(),
                    updated.updatedAt(),
                    updated.characterState(),
                    updated.scene(),
                    updated.relationships(),
                    updated.events(),
                    updated.memories(),
                    updated.messages(),
                    updated.playerPersonaId(),
                    updated.storyId(),
                    List.of(updated.characterId()),
                    updated.playerPersonaState());
            needsSave = true;
        }

        if (updated.characterState() != null && updated.scene() != null) {
            CharacterRuntimeState reconciled = storyStateService.reconcileCharacterLocation(
                    updated.characterState(), updated.scene());
            if (reconciled != null && reconciled.location() != null
                    && !reconciled.location().equals(updated.characterState().location())) {
                updated = updated.withCharacterState(reconciled);
                needsSave = true;
            }
        }

        if (needsSave) {
            return storage.saveConversation(updated);
        }
        return conversation;
    }

    private List<Relationship> migrateRelationships(Conversation conversation) {
        List<Relationship> migrated = new ArrayList<>();
        for (Relationship relationship : conversation.relationships()) {
            String sourceId = relationship.sourceId() != null
                    ? relationship.sourceId()
                    : conversation.characterId();
            migrated.add(relationship.withSourceId(sourceId));
        }
        return List.copyOf(migrated);
    }
}
