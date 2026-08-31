package com.aditya.roleplay.visual;

import com.aditya.roleplay.exception.RoleplayException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local development provider that uses the canonical reference image as the generation output.
 * Demonstrates the full pipeline without calling an external image API.
 */
@ApplicationScoped
public class LocalStubImageGenerationClient {

    @ConfigProperty(name = "roleplay.visual.provider", defaultValue = "openai")
    String providerName;

    public ImageGenerationResponse generate(ImageGenerationRequest request) {
        return generate(request, null);
    }

    public ImageGenerationResponse generate(ImageGenerationRequest request, String apiKeyOverride) {
        if (request.referenceImagePaths().isEmpty()) {
            throw new RoleplayException(
                    "Scene image generation requires at least one canonical reference image.",
                    "VISUAL_GENERATION_ERROR",
                    400);
        }

        Path referencePath = Path.of(request.referenceImagePaths().get(0));
        if (!Files.exists(referencePath)) {
            throw new RoleplayException(
                    "Canonical reference image not found: " + referencePath,
                    "VISUAL_REFERENCE_NOT_FOUND",
                    404);
        }

        try {
            byte[] bytes = Files.readAllBytes(referencePath);
            String mimeType = Files.probeContentType(referencePath);
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }
            return new ImageGenerationResponse(bytes, mimeType, providerName, request.model());
        } catch (IOException e) {
            throw new RoleplayException(
                    "Failed to read reference image for generation: " + e.getMessage(),
                    "VISUAL_GENERATION_ERROR",
                    500);
        }
    }
}
