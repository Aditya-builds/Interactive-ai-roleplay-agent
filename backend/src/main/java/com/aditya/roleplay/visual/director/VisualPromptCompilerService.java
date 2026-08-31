package com.aditya.roleplay.visual.director;

import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.visual.director.VisualPlanCharacter;
import com.aditya.roleplay.model.visual.director.VisualScenePlan;
import com.aditya.roleplay.service.CharacterService;
import com.aditya.roleplay.visual.ImageGenerationRequest;
import com.aditya.roleplay.visual.VisualIdentityService;
import com.aditya.roleplay.visual.VisualImageStorageService;
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

    public ImageGenerationRequest compile(
            VisualScenePlan plan,
            RoleplayCharacter focalCharacter,
            int width,
            int height,
            String aspectRatio,
            String model,
            VisualImageStorageService storage) {

        List<String> referencePaths = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (VisualPlanCharacter planCharacter : plan.characters()) {
            if ("user".equals(planCharacter.characterId()) || "player".equals(planCharacter.characterId())) {
                continue;
            }
            RoleplayCharacter character = characterService.requireCharacter(planCharacter.characterId());
            for (String path : visualIdentityService.resolveReferenceImagePaths(character, storage)) {
                if (seen.add(path)) {
                    referencePaths.add(path);
                }
            }
        }

        if (referencePaths.isEmpty()) {
            for (String path : visualIdentityService.resolveReferenceImagePaths(focalCharacter, storage)) {
                if (seen.add(path)) {
                    referencePaths.add(path);
                }
            }
        }

        return new ImageGenerationRequest(
                plan.prompt(),
                plan.negativePrompt(),
                referencePaths,
                aspectRatio,
                width,
                height,
                null,
                model);
    }
}
