package com.aditya.roleplay.visual;

import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.visual.CharacterVisualIdentity;
import com.aditya.roleplay.model.visual.VisualCharacterScenePresence;
import com.aditya.roleplay.model.visual.VisualSceneState;
import com.aditya.roleplay.model.visual.reference.ReferenceSelectionResult;
import com.aditya.roleplay.visual.reference.ReferenceImagePromptEnhancer;
import com.aditya.roleplay.visual.reference.ReferenceImageSelectionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class VisualPromptService {

    @Inject
    VisualIdentityService visualIdentityService;

    @Inject
    ReferenceImageSelectionService referenceImageSelectionService;

    @Inject
    ReferenceImagePromptEnhancer referenceImagePromptEnhancer;

    public ImageGenerationRequest buildRequest(
            RoleplayCharacter focalCharacter,
            VisualSceneState sceneState,
            int width,
            int height,
            String aspectRatio,
            String model,
            VisualImageStorageService storage) {

        CharacterVisualIdentity identity = visualIdentityService.resolve(focalCharacter);
        VisualCharacterScenePresence focalPresence = focalPresence(sceneState, focalCharacter.name());
        ReferenceSelectionResult selection = referenceImageSelectionService.select(
                focalCharacter, identity, sceneState, focalPresence, storage);

        String prompt = referenceImagePromptEnhancer.enhance(
                buildStructuredPrompt(focalCharacter.name(), identity, sceneState, focalPresence, selection),
                focalCharacter.id(),
                selection);
        String negativePrompt = buildNegativePrompt(identity);

        return new ImageGenerationRequest(
                prompt,
                negativePrompt,
                selection.filesystemPaths(),
                aspectRatio,
                width,
                height,
                null,
                model,
                selection.selectedReferenceIds(),
                selection.selectionSummary());
    }

    private String buildStructuredPrompt(
            String characterName,
            CharacterVisualIdentity identity,
            VisualSceneState sceneState,
            VisualCharacterScenePresence focalPresence,
            ReferenceSelectionResult selection) {

        String accessories = identity.accessories().isEmpty()
                ? "none"
                : String.join(", ", identity.accessories());

        String sceneClothing = focalPresence.sceneClothing() != null && !focalPresence.sceneClothing().isBlank()
                ? focalPresence.sceneClothing()
                : defaultValue(identity.clothingDescription(), "canonical outfit from reference image");

        String referenceInstruction = selection.selectedReferenceIds().isEmpty()
                ? "Use the canonical reference image as the primary source of truth for this character's appearance."
                : """
                Use the supplied %s reference images as visual identity references for %s.
                Preserve %s's identity consistently across the generated image: face, facial proportions, eyes, hair color, hairstyle, hair length, skin tone, body proportions, and overall character design.
                Use pose, camera angle, expression, and composition references as guidance only.
                Do not copy unrelated scene details from the reference images when they conflict with the current scene description below.
                Selected references: %s
                """.formatted(
                        selection.selectedReferenceIds().size(),
                        characterName,
                        characterName,
                        selection.selectionSummary());

        return """
                CHARACTER IDENTITY — LOCKED
                Character: %s
                Overall: %s
                Face: %s
                Hair: %s
                Eyes: %s
                Skin: %s
                Body: %s
                Canonical clothing style: %s
                Accessories: %s
                Identity attributes above must remain stable regardless of pose, camera, or scene.

                REFERENCE IMAGE INSTRUCTIONS
                %s

                CURRENT SCENE
                Location: %s (%s)
                Time: %s
                Situation: %s
                Characters present: %s

                RECENT CHAT MOMENT (MUST BE VISUALLY REFLECTED)
                %s

                CURRENT ACTION / POSE
                %s is %s with pose: %s

                EXPRESSION
                %s

                CLOTHING
                %s

                ENVIRONMENT
                %s

                CAMERA
                %s

                LIGHTING
                %s

                ART STYLE
                %s
                """.formatted(
                characterName,
                defaultValue(identity.visualDescription(), characterName),
                defaultValue(identity.faceDescription(), "match reference face"),
                defaultValue(identity.hairDescription(), "match reference hair"),
                defaultValue(identity.eyeDescription(), "match reference eyes"),
                defaultValue(identity.skinDescription(), "match reference skin tone"),
                defaultValue(identity.bodyDescription(), "match reference body proportions"),
                defaultValue(identity.clothingDescription(), "match canonical reference outfit"),
                accessories,
                referenceInstruction.trim(),
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

    private static VisualCharacterScenePresence focalPresence(VisualSceneState sceneState, String characterName) {
        return sceneState.characters().stream()
                .findFirst()
                .orElse(new VisualCharacterScenePresence(
                        characterName.toLowerCase(),
                        characterName,
                        "standing",
                        "neutral",
                        "present",
                        null));
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
