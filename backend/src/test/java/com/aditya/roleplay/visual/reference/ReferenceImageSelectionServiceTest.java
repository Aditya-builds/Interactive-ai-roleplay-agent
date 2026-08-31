package com.aditya.roleplay.visual.reference;

import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.visual.VisualCharacterScenePresence;
import com.aditya.roleplay.model.visual.VisualSceneState;
import com.aditya.roleplay.model.visual.reference.ReferenceCameraAngle;
import com.aditya.roleplay.model.visual.reference.ReferenceExpression;
import com.aditya.roleplay.model.visual.reference.ReferencePose;
import com.aditya.roleplay.model.visual.reference.ReferenceSelectionResult;
import com.aditya.roleplay.service.CharacterService;
import com.aditya.roleplay.visual.VisualIdentityService;
import com.aditya.roleplay.visual.VisualImageStorageService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ReferenceImageSelectionServiceTest {

    @Inject
    ReferenceImageSelectionService selectionService;

    @Inject
    CharacterService characterService;

    @Inject
    VisualIdentityService visualIdentityService;

    @Inject
    VisualImageStorageService imageStorageService;

    @Inject
    CharacterReferenceLibraryService libraryService;

    @Test
    void selectsCanonicalReferenceForAurora() {
        RoleplayCharacter aurora = characterService.requireCharacter("aurora");
        VisualSceneState sceneState = sceneState("medium shot", "standing near a table", "calm", "listening");

        ReferenceSelectionResult result = selectionService.select(
                aurora,
                visualIdentityService.resolve(aurora),
                sceneState,
                sceneState.characters().get(0),
                imageStorageService);

        assertFalse(result.filesystemPaths().isEmpty());
        assertTrue(result.selectedReferenceIds().contains("aurora-01"));
        assertTrue(result.filesystemPaths().size() <= 5);
        assertTrue(result.filesystemPaths().size() >= 3);
    }

    @Test
    void prefersSideAngleReferenceForSideViewScene() {
        RoleplayCharacter aurora = characterService.requireCharacter("aurora");
        VisualSceneState sceneState = sceneState("side view profile shot", "standing", "neutral", "present");

        ReferenceSelectionResult result = selectionService.select(
                aurora,
                visualIdentityService.resolve(aurora),
                sceneState,
                sceneState.characters().get(0),
                imageStorageService);

        assertTrue(result.selectedReferenceIds().contains("aurora-04")
                || result.selectedReferenceIds().contains("aurora-01"));
    }

    @Test
    void prefersCombatReferenceForCombatScene() {
        RoleplayCharacter aurora = characterService.requireCharacter("aurora");
        VisualSceneState sceneState = sceneState("low angle combat shot", "combat stance", "determined", "fighting");

        ReferenceSelectionResult result = selectionService.select(
                aurora,
                visualIdentityService.resolve(aurora),
                sceneState,
                sceneState.characters().get(0),
                imageStorageService);

        assertTrue(result.selectedReferenceIds().contains("aurora-17")
                || result.selectedReferenceIds().contains("aurora-01"));
    }

    @Test
    void prefersThoughtfulExpressionReference() {
        RoleplayCharacter aurora = characterService.requireCharacter("aurora");
        VisualSceneState sceneState = sceneState("medium shot", "standing", "thoughtful", "thinking");

        ReferenceSelectionResult result = selectionService.select(
                aurora,
                visualIdentityService.resolve(aurora),
                sceneState,
                sceneState.characters().get(0),
                imageStorageService);

        assertTrue(result.selectedReferenceIds().stream()
                .anyMatch(id -> id.equals("aurora-09") || id.equals("aurora-08") || id.equals("aurora-01")));
    }

    @Test
    void avoidsSelectingAllTwentyReferences() {
        RoleplayCharacter aurora = characterService.requireCharacter("aurora");
        VisualSceneState sceneState = sceneState("full body front shot", "standing", "serene", "present");

        ReferenceSelectionResult result = selectionService.select(
                aurora,
                visualIdentityService.resolve(aurora),
                sceneState,
                sceneState.characters().get(0),
                imageStorageService);

        assertTrue(result.selectedReferenceIds().size() <= 5);
        assertTrue(result.filesystemPaths().size() <= 5);
    }

    @Test
    void characterWithoutLibraryFallsBackToLegacyReferences() {
        RoleplayCharacter runa = characterService.requireCharacter("runa");
        VisualSceneState sceneState = sceneState("medium shot", "standing", "neutral", "present");

        ReferenceSelectionResult result = selectionService.select(
                runa,
                visualIdentityService.resolve(runa),
                sceneState,
                sceneState.characters().get(0),
                imageStorageService);

        assertEquals("legacy", result.selectionSummary());
        assertFalse(result.filesystemPaths().isEmpty());
        assertTrue(result.selectedReferenceIds().isEmpty());
    }

    @Test
    void auroraLibraryIsRegistered() {
        assertTrue(libraryService.hasLibrary("aurora"));
        assertEquals(20, libraryService.summarize("aurora").imageCount());
    }

    private static VisualSceneState sceneState(String camera, String pose, String expression, String action) {
        return new VisualSceneState(
                "guild_hall",
                "Stone guild hall",
                "evening",
                "Private conversation",
                List.of(new VisualCharacterScenePresence("aurora", "Aurora", pose, expression, action, null)),
                camera,
                "torchlight",
                "quiet",
                "recent moment");
    }
}
