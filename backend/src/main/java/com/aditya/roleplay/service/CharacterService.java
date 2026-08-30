package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.CharacterDetailResponse;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.World;
import com.aditya.roleplay.storage.JsonStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

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
}
