package com.aditya.roleplay.visual.director;

import com.aditya.roleplay.model.visual.director.InteractionPlan;
import com.aditya.roleplay.model.visual.director.SceneCompositionPlan;
import com.aditya.roleplay.model.visual.director.VisualPlanCharacter;
import com.aditya.roleplay.model.visual.director.VisualScenePlan;
import com.aditya.roleplay.service.CharacterService;
import com.aditya.roleplay.visual.ImageGenerationRequest;
import com.aditya.roleplay.visual.VisualImageStorageService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class VisualPromptCompilerServiceTest {

    @Inject
    VisualPromptCompilerService compilerService;

    @Inject
    CharacterService characterService;

    @Inject
    VisualImageStorageService storage;

    @Test
    void compilesDirectorPlanIntoImageRequestWithAuroraReference() {
        var character = characterService.requireCharacter("aurora");
        var plan = new VisualScenePlan(
                true,
                "EMOTIONAL_INTERACTION",
                "test",
                List.of(new VisualPlanCharacter(
                        "aurora",
                        "Aurora",
                        "/api/visuals/references/aurora",
                        "standing",
                        "guarded",
                        "talking",
                        "left foreground",
                        null)),
                new SceneCompositionPlan(
                        "guild_hall",
                        "Guild hall",
                        "evening",
                        "Guild hall",
                        "warm light",
                        "tense",
                        "medium-wide shot",
                        "cinematic",
                        "foreground interaction",
                        "background tables"),
                new InteractionPlan("Aurora and Laxus", "2 meters", "facing each other", "moderate"),
                "CHARACTER REFERENCES\n- Aurora",
                "different face, blonde hair",
                12);

        ImageGenerationRequest request = compilerService.compile(
                plan, character, 1024, 576, "16:9", "gpt-image-2", storage);

        assertTrue(request.prompt().contains("CHARACTER REFERENCES"));
        assertFalse(request.referenceImagePaths().isEmpty());
        assertTrue(request.negativePrompt().contains("different face"));
    }
}
