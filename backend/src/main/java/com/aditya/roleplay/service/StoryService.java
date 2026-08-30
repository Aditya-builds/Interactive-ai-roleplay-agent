package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.storage.JsonStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class StoryService {

    @Inject
    JsonStorageService storage;

    public List<Story> listStories() {
        return storage.loadStories();
    }

    public Story requireStory(String id) {
        return storage.loadStory(id)
                .orElseThrow(() -> new RoleplayException("Story not found: " + id, "STORY_NOT_FOUND", 404));
    }
}
