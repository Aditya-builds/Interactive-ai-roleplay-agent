package com.aditya.roleplay.visual.reference;

import com.aditya.roleplay.model.visual.reference.CharacterReferenceImage;
import com.aditya.roleplay.model.visual.reference.ReferenceSelectionResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
@ApplicationScoped
public class ReferenceImagePromptEnhancer {

    @Inject
    CharacterReferenceLibraryService libraryService;

    public String enhance(String basePrompt, String characterId, ReferenceSelectionResult selection) {
        if (selection.selectedImages().isEmpty()) {
            return enhance(basePrompt, characterId, selection.selectedReferenceIds());
        }

        StringBuilder builder = new StringBuilder();
        builder.append("MULTI-REFERENCE CHARACTER GENERATION\n");
        builder.append(characterId).append(" identity is defined by the attached reference images.\n\n");

        int index = 1;
        for (CharacterReferenceImage image : selection.selectedImages()) {
            builder.append("Image ").append(index).append(" — ").append(image.id()).append(": ");
            builder.append(describeImage(image)).append('\n');
            index++;
        }

        builder.append("""

                RULES
                - Image 1 is the primary identity anchor for face, hair, eyes, skin, and body proportions.
                - Use other images only for pose, angle, expression, or outfit guidance.
                - Generate a NEW scene from the description below.
                - Do NOT copy backgrounds, signs, text, props, or unrelated details from references.
                - Aurora must have jet-black straight hair, pale porcelain skin, dark almond eyes, slender athletic build, and her ethereal cream lace outfit unless scene clothing overrides it.

                """);

        builder.append(basePrompt);
        return builder.toString();
    }

    public String enhance(String basePrompt, String characterId, List<String> referenceIds) {
        var library = libraryService.findLibrary(characterId);
        if (library.isEmpty() || referenceIds.isEmpty()) {
            return basePrompt;
        }

        List<CharacterReferenceImage> images = referenceIds.stream()
                .map(id -> library.get().findById(id).orElse(null))
                .filter(image -> image != null)
                .toList();

        if (images.isEmpty()) {
            return basePrompt;
        }

        return enhance(basePrompt, characterId, new ReferenceSelectionResult(images, List.of(), "indexed"));
    }

    private static String describeImage(CharacterReferenceImage image) {
        StringBuilder description = new StringBuilder();
        if (image.identityAnchor() || image.imageType().name().equals("CANONICAL")) {
            description.append("identity anchor; ");
        }
        description.append(image.imageType().name().toLowerCase().replace('_', ' '));
        if (image.pose() != null && !image.pose().name().equals("UNKNOWN")) {
            description.append(", pose=").append(image.pose().name().toLowerCase());
        }
        if (image.cameraAngle() != null && !image.cameraAngle().name().equals("UNKNOWN")) {
            description.append(", camera=").append(image.cameraAngle().name().toLowerCase().replace('_', ' '));
        }
        if (image.expression() != null && !image.expression().name().equals("UNKNOWN")) {
            description.append(", expression=").append(image.expression().name().toLowerCase());
        }
        if (image.notes() != null && !image.notes().isBlank()) {
            description.append(" — ").append(image.notes());
        }
        return description.toString();
    }
}
