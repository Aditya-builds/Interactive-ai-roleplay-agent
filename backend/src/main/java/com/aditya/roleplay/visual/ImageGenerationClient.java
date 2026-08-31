package com.aditya.roleplay.visual;

public interface ImageGenerationClient {
    ImageGenerationResponse generate(ImageGenerationRequest request);

    default ImageGenerationResponse generate(ImageGenerationRequest request, String apiKeyOverride) {
        return generate(request);
    }
}
