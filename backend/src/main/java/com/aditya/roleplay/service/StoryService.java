package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.CreateStoryRequest;
import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.storage.JsonStorageService;
import com.aditya.roleplay.util.SlugIdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class StoryService {

    @Inject
    JsonStorageService storage;

    @Inject
    CharacterService characterService;

    public List<Story> listStories() {
        return storage.loadStories();
    }

    public Story requireStory(String id) {
        return storage.loadStory(id)
                .orElseThrow(() -> new RoleplayException("Story not found: " + id, "STORY_NOT_FOUND", 404));
    }

    public Story createStory(CreateStoryRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new RoleplayException("Story title is required", "INVALID_REQUEST", 400);
        }
        if (request.worldId() == null || request.worldId().isBlank()) {
            throw new RoleplayException("worldId is required", "INVALID_REQUEST", 400);
        }

        String storyId = request.id() != null && !request.id().isBlank()
                ? request.id().trim()
                : SlugIdGenerator.fromName(request.title());
        if (storage.loadStory(storyId).isPresent()) {
            storyId = SlugIdGenerator.uniqueFromName(request.title(), id -> storage.loadStory(id).isPresent());
        }

        List<String> startingCharacterIds = resolveStartingCharacters(request);

        Story story = new Story(
                storyId,
                request.title().trim(),
                request.worldId().trim(),
                request.premise() != null ? request.premise().trim() : "",
                request.openingNarrative() != null ? request.openingNarrative().trim() : "",
                startingCharacterIds,
                request.startingLocation() != null ? request.startingLocation().trim() : "unknown",
                request.storyRules() != null ? request.storyRules() : List.of(),
                List.of());

        return storage.saveStory(story);
    }

    private List<String> resolveStartingCharacters(CreateStoryRequest request) {
        List<String> names = new ArrayList<>();
        if (request.startingCharacterNames() != null) {
            names.addAll(request.startingCharacterNames().stream()
                    .filter(name -> name != null && !name.isBlank())
                    .map(String::trim)
                    .toList());
        }
        if (names.isEmpty() && request.startingCharacters() != null) {
            names.addAll(request.startingCharacters().stream()
                    .filter(name -> name != null && !name.isBlank())
                    .map(String::trim)
                    .toList());
        }

        List<String> ids = new ArrayList<>();
        for (String name : names) {
            ids.add(characterService.ensureCharacterByName(name, request.worldId()));
        }
        return List.copyOf(ids);
    }
}
