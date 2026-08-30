package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.CharacterDetailResponse;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.SeedMemory;
import com.aditya.roleplay.model.StoryMemoryEntry;
import com.aditya.roleplay.model.World;
import com.aditya.roleplay.storage.JsonStorageService;
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

    public Set<String> allowedRelationshipTargets(String worldId, String conversationCharacterId) {
        Set<String> targets = new HashSet<>();
        targets.add("user");
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
