package com.aditya.roleplay.visual;

import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.visual.CharacterVisualIdentity;
import com.aditya.roleplay.model.visual.VisualCharacterScenePresence;
import com.aditya.roleplay.model.visual.VisualSceneState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class VisualPromptService {

    @Inject
    VisualIdentityService visualIdentityService;

    public ImageGenerationRequest buildRequest(
            RoleplayCharacter focalCharacter,
            VisualSceneState sceneState,
            int width,
            int height,
            String aspectRatio,
            String model,
            VisualImageStorageService storage) {

        CharacterVisualIdentity identity = visualIdentityService.resolve(focalCharacter);
        List<String> referencePaths = visualIdentityService.resolveReferenceImagePaths(focalCharacter, storage);

        String prompt = buildStructuredPrompt(focalCharacter.name(), identity, sceneState);
        String negativePrompt = buildNegativePrompt(identity);

        return new ImageGenerationRequest(
                prompt,
                negativePrompt,
                referencePaths,
                aspectRatio,
                width,
                height,
                null,
                model);
    }

    private String buildStructuredPrompt(
            String characterName,
            CharacterVisualIdentity identity,
            VisualSceneState sceneState) {

        VisualCharacterScenePresence focalPresence = sceneState.characters().stream()
                .findFirst()
                .orElse(new VisualCharacterScenePresence(characterName.toLowerCase(), characterName,
                        "standing", "neutral", "present", null));

        String accessories = identity.accessories().isEmpty()
                ? "none"
                : String.join(", ", identity.accessories());

        String sceneClothing = focalPresence.sceneClothing() != null && !focalPresence.sceneClothing().isBlank()
                ? focalPresence.sceneClothing()
                : defaultValue(identity.clothingDescription(), "canonical outfit from reference image");

        return """
                CHARACTER IDENTITY (LOCKED — MUST MATCH CANONICAL REFERENCE IMAGE)
                Character: %s
                Overall: %s
                Face: %s
                Hair: %s
                Eyes: %s
                Skin: %s
                Body: %s
                Canonical clothing style: %s
                Accessories: %s
                Use the canonical reference image as the primary source of truth for this character's appearance.

                SCENE
                Location: %s (%s)
                Time: %s
                Situation: %s
                Characters present: %s

                RECENT CHAT MOMENT (MUST BE VISUALLY REFLECTED)
                %s

                POSE / ACTION
                %s is %s with pose: %s

                EXPRESSION
                %s

                CLOTHING (SCENE — may differ from canonical only when explicitly set)
                %s

                ENVIRONMENT
                %s

                CAMERA
                %s

                LIGHTING
                %s

                STYLE
                %s
                """.formatted(
                characterName,
                defaultValue(identity.visualDescription(), characterName),
                defaultValue(identity.faceDescription(), "match canonical reference face"),
                defaultValue(identity.hairDescription(), "match canonical reference hair"),
                defaultValue(identity.eyeDescription(), "match canonical reference eyes"),
                defaultValue(identity.skinDescription(), "match canonical reference skin tone"),
                defaultValue(identity.bodyDescription(), "match canonical reference body proportions"),
                defaultValue(identity.clothingDescription(), "match canonical reference outfit"),
                accessories,
                sceneState.location(),
                defaultValue(sceneState.locationDescription(), sceneState.location()),
                defaultValue(sceneState.time(), "unspecified"),
                defaultValue(sceneState.situation(), "ongoing roleplay scene"),
                formatCharactersPresent(sceneState),
                defaultValue(sceneState.recentMoment(), "depict the current roleplay moment from the situation above"),
                focalPresence.name(),
                defaultValue(focalPresence.action(), "in the scene"),
                defaultValue(focalPresence.pose(), "natural stance"),
                defaultValue(focalPresence.expression(), "neutral"),
                sceneClothing,
                defaultValue(sceneState.locationDescription(), sceneState.location()),
                defaultValue(sceneState.camera(), "medium shot"),
                defaultValue(sceneState.lighting(), "cinematic lighting"),
                defaultValue(identity.artStyle(), "dark fantasy illustration"));
    }

    private String buildNegativePrompt(CharacterVisualIdentity identity) {
        String base = "different face, different facial structure, different eye color, different hair color, "
                + "different hair length, different hairstyle, different skin tone, different body proportions, "
                + "inconsistent character, duplicate characters, blurry, low quality, watermark, text";
        if (identity.negativePrompt() != null && !identity.negativePrompt().isBlank()) {
            return base + ", " + identity.negativePrompt();
        }
        return base;
    }

    private static String formatCharactersPresent(VisualSceneState sceneState) {
        return sceneState.characters().stream()
                .map(VisualCharacterScenePresence::name)
                .collect(Collectors.joining(", "));
    }

    private static String defaultValue(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
