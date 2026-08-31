package com.aditya.roleplay.visual;

import com.aditya.roleplay.model.visual.VisualCharacterScenePresence;
import com.aditya.roleplay.model.visual.VisualSceneState;
import com.aditya.roleplay.service.CharacterService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class VisualPromptServiceTest {

    @Inject
    VisualPromptService visualPromptService;

    @Inject
    CharacterService characterService;

    @Inject
    VisualImageStorageService imageStorageService;

    @Test
    void buildsStructuredPromptWithLockedIdentitySection() {
        var character = characterService.requireCharacter("aurora");
        var sceneState = new VisualSceneState(
                "guild_hall",
                "A grand stone guild hall with pillars and torchlight",
                "late evening",
                "Aurora and the player are talking privately",
                List.of(new VisualCharacterScenePresence(
                        "aurora", "Aurora", "standing near a table", "calm", "listening", null)),
                "medium shot, eye level",
                "warm torchlight",
                "quiet tense atmosphere",
                "Aurora: hi\nAurora: thanks for meeting me");

        ImageGenerationRequest request = visualPromptService.buildRequest(
                character,
                sceneState,
                1024,
                576,
                "16:9",
                "local-stub",
                imageStorageService);

        assertTrue(request.prompt().contains("CHARACTER IDENTITY — LOCKED"));
        assertTrue(request.prompt().contains("MULTI-REFERENCE CHARACTER GENERATION"));
        assertTrue(request.prompt().contains("Image 1"));
        assertTrue(request.prompt().contains("CURRENT SCENE"));
        assertTrue(request.prompt().contains("CURRENT ACTION / POSE"));
        assertTrue(request.prompt().contains("EXPRESSION"));
        assertTrue(request.prompt().contains("CAMERA"));
        assertTrue(request.prompt().contains("LIGHTING"));
        assertTrue(request.prompt().contains("ART STYLE"));
        assertTrue(request.prompt().contains("guild_hall"));
        assertTrue(request.prompt().contains("RECENT CHAT MOMENT"));
        assertTrue(request.prompt().contains("thanks for meeting me"));
        assertTrue(request.negativePrompt().contains("different face"));
        assertTrue(request.referenceImagePaths().size() <= 5);
        assertTrue(request.referenceImagePaths().size() >= 3);
        assertTrue(request.selectedReferenceIds().contains("aurora-01"));
    }
}
