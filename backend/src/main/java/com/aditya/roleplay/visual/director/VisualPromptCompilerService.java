package com.aditya.roleplay.visual.director;

import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.visual.VisualCharacterScenePresence;
import com.aditya.roleplay.model.visual.VisualSceneState;
import com.aditya.roleplay.model.visual.director.SceneCompositionPlan;
import com.aditya.roleplay.model.visual.director.VisualPlanCharacter;
import com.aditya.roleplay.model.visual.director.VisualScenePlan;
import com.aditya.roleplay.model.visual.reference.ReferenceSelectionResult;
import com.aditya.roleplay.service.CharacterService;
import com.aditya.roleplay.visual.ImageGenerationRequest;
import com.aditya.roleplay.visual.VisualIdentityService;
import com.aditya.roleplay.visual.VisualImageStorageService;
import com.aditya.roleplay.visual.reference.ReferenceImagePromptEnhancer;
import com.aditya.roleplay.visual.reference.ReferenceImageSelectionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class VisualPromptCompilerService {

    @Inject
    CharacterService characterService;

    @Inject
    VisualIdentityService visualIdentityService;

    @Inject
    ReferenceImageSelectionService referenceImageSelectionService;

    @Inject
    ReferenceImagePromptEnhancer referenceImagePromptEnhancer;

    public ImageGenerationRequest compile(
            VisualScenePlan plan,
            RoleplayCharacter focalCharacter,
            int width,
            int height,
            String aspectRatio,
            String model,
            VisualImageStorageService storage) {

        List<String> referencePaths = new ArrayList<>();
        List<String> selectedReferenceIds = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<String> summaries = new ArrayList<>();

        VisualSceneState sceneState = toSceneState(plan);
        for (VisualPlanCharacter planCharacter : plan.characters()) {
            if ("user".equals(planCharacter.characterId()) || "player".equals(planCharacter.characterId())) {
                continue;
            }
            RoleplayCharacter character = characterService.requireCharacter(planCharacter.characterId());
            VisualCharacterScenePresence presence = toPresence(planCharacter);
            ReferenceSelectionResult selection = referenceImageSelectionService.select(
                    character,
                    visualIdentityService.resolve(character),
                    sceneState,
                    presence,
                    storage);
            for (String path : selection.filesystemPaths()) {
                if (seen.add(path)) {
                    referencePaths.add(path);
                }
            }
            selectedReferenceIds.addAll(selection.selectedReferenceIds());
            if (selection.selectionSummary() != null && !selection.selectionSummary().isBlank()) {
                summaries.add(character.id() + ": " + selection.selectionSummary());
            }
        }

        if (referencePaths.isEmpty()) {
            ReferenceSelectionResult fallback = referenceImageSelectionService.select(
                    focalCharacter,
                    visualIdentityService.resolve(focalCharacter),
                    sceneState,
                    toPresence(plan.characters().isEmpty()
                            ? new VisualPlanCharacter(focalCharacter.id(), focalCharacter.name(), null,
                                    "standing", "neutral", "present", null, null)
                            : plan.characters().get(0)),
                    storage);
            referencePaths.addAll(fallback.filesystemPaths());
            selectedReferenceIds.addAll(fallback.selectedReferenceIds());
            summaries.add(fallback.selectionSummary());
        }

        return new ImageGenerationRequest(
                referenceImagePromptEnhancer.enhance(
                        plan.prompt(),
                        focalCharacter.id(),
                        selectedReferenceIds),
                plan.negativePrompt(),
                referencePaths,
                aspectRatio,
                width,
                height,
                null,
                model,
                selectedReferenceIds,
                String.join("; ", summaries));
    }

    private static VisualSceneState toSceneState(VisualScenePlan plan) {
        SceneCompositionPlan scene = plan.scene();
        if (scene == null) {
            return new VisualSceneState(
                    "unknown",
                    "unknown",
                    null,
                    null,
                    plan.characters().stream().map(VisualPromptCompilerService::toPresence).toList(),
                    null,
                    null,
                    null,
                    null);
        }
        return new VisualSceneState(
                defaultValue(scene.location(), "unknown"),
                defaultValue(scene.locationDescription(), scene.location()),
                scene.time(),
                scene.environment(),
                plan.characters().stream().map(VisualPromptCompilerService::toPresence).toList(),
                scene.camera(),
                scene.lighting(),
                scene.atmosphere(),
                null);
    }

    private static VisualCharacterScenePresence toPresence(VisualPlanCharacter planCharacter) {
        return new VisualCharacterScenePresence(
                planCharacter.characterId(),
                planCharacter.name(),
                planCharacter.pose(),
                planCharacter.expression(),
                planCharacter.action(),
                planCharacter.sceneClothing());
    }

    private static String defaultValue(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
