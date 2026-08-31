package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.CharacterDetailResponse;
import com.aditya.roleplay.model.CharacterHealth;
import com.aditya.roleplay.model.CharacterPresence;
import com.aditya.roleplay.model.CreateCharacterRequest;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.SeedMemory;
import com.aditya.roleplay.model.StoryMemoryEntry;
import com.aditya.roleplay.model.World;
import com.aditya.roleplay.storage.JsonStorageService;
import com.aditya.roleplay.util.SlugIdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CharacterService {

    @Inject
    JsonStorageService storage;

    public List<RoleplayCharacter> listCharacters() {
        return storage.loadCharacters();
    }

    public CharacterDetailResponse getCharacterDetail(String id) {
        RoleplayCharacter character = storage.loadCharacter(id)
                .orElseThrow(() -> new RoleplayException("Character not found: " + id, "CHARACTER_NOT_FOUND", 404));

        World world = storage.loadWorld(character.worldId())
                .orElseThrow(() -> new RoleplayException("World not found: " + character.worldId(), "WORLD_NOT_FOUND", 404));

        return new CharacterDetailResponse(character, world);
    }

    public RoleplayCharacter requireCharacter(String id) {
        return storage.loadCharacter(id)
                .orElseThrow(() -> new RoleplayException("Character not found: " + id, "CHARACTER_NOT_FOUND", 404));
    }

    public World requireWorld(String id) {
        return storage.loadWorld(id)
                .orElseThrow(() -> new RoleplayException("World not found: " + id, "WORLD_NOT_FOUND", 404));
    }

    public RoleplayCharacter createCharacter(CreateCharacterRequest request) {
        if (request.worldId() == null || request.worldId().isBlank()) {
            throw new RoleplayException("worldId is required", "INVALID_REQUEST", 400);
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new RoleplayException("Character name is required", "INVALID_REQUEST", 400);
        }
        requireWorld(request.worldId());

        String characterId = request.id() != null && !request.id().isBlank()
                ? request.id().trim()
                : SlugIdGenerator.fromName(request.name());
        if (storage.loadCharacter(characterId).isPresent()) {
            characterId = SlugIdGenerator.uniqueFromName(request.name(), id -> storage.loadCharacter(id).isPresent());
        }

        RoleplayCharacter character = new RoleplayCharacter(
                characterId,
                request.worldId().trim(),
                request.name().trim(),
                request.imageUrl(),
                new CharacterHealth(100, 100),
                request.personality() != null ? request.personality() : List.of(),
                request.background() != null ? request.background().trim() : "",
                request.speakingStyle() != null ? request.speakingStyle().trim() : "",
                List.of(),
                new CharacterPresence("unknown", "unspecified", "Present in the world."),
                List.of(),
                List.of(),
                request.openingMessage() != null ? request.openingMessage().trim() : "",
                List.of(),
                List.of(),
                null,
                null);

        return storage.saveCharacter(character);
    }

    public String ensureCharacterByName(String name, String worldId) {
        String trimmedName = name.trim();
        for (RoleplayCharacter existing : storage.loadCharacters()) {
            if (existing.name().equalsIgnoreCase(trimmedName)) {
                return existing.id();
            }
        }
        CreateCharacterRequest request = new CreateCharacterRequest(
                null,
                worldId,
                trimmedName,
                "",
                "",
                List.of(),
                "",
                null);
        return createCharacter(request).id();
    }

    public List<String> listWorldIds() {
        return storage.listWorldIds();
    }

    public Set<String> allowedRelationshipTargets(String worldId, String conversationCharacterId) {
        return allowedRelationshipTargets(worldId, conversationCharacterId, null);
    }

    public Set<String> allowedRelationshipTargets(String worldId, String conversationCharacterId, String playerPersonaId) {
        Set<String> targets = new HashSet<>();
        targets.add("user");
        if (playerPersonaId != null && !playerPersonaId.isBlank()) {
            targets.add(playerPersonaId);
        }
        for (RoleplayCharacter character : storage.loadCharacters()) {
            if (worldId.equals(character.worldId()) && !character.id().equals(conversationCharacterId)) {
                targets.add(character.id());
            }
        }
        return targets;
    }

    public List<StoryMemoryEntry> seedMemoriesForCharacter(RoleplayCharacter character, Instant now) {
        return character.seedMemories().stream()
                .map(seed -> toMemoryEntry(seed, now))
                .collect(Collectors.toList());
    }

    public List<Message> initialMessagesForCharacter(RoleplayCharacter character, Instant now) {
        if (character.openingMessage() == null || character.openingMessage().isBlank()) {
            return List.of();
        }
        return List.of(new Message(
                UUID.randomUUID().toString(),
                Role.ASSISTANT,
                character.openingMessage().trim(),
                now));
    }

    private StoryMemoryEntry toMemoryEntry(SeedMemory seed, Instant now) {
        return new StoryMemoryEntry(
                UUID.randomUUID().toString(),
                seed.content(),
                now,
                "seed",
                seed.importance() != null ? seed.importance() : 0.6,
                seed.tags(),
                seed.relatedCharacterIds());
    }
}
