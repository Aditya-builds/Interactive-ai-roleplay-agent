package com.aditya.roleplay.visual;

import com.aditya.roleplay.visual.openai.OpenAiCompatibleImageGenerationClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ImageGenerationClientRouter implements ImageGenerationClient {

    @Inject
    OpenAiCompatibleImageGenerationClient openAiClient;

    @Inject
    LocalStubImageGenerationClient localStubClient;

    @ConfigProperty(name = "roleplay.visual.provider", defaultValue = "openai")
    String provider;

    @Override
    public ImageGenerationResponse generate(ImageGenerationRequest request) {
        return generate(request, null);
    }

    @Override
    public ImageGenerationResponse generate(ImageGenerationRequest request, String apiKeyOverride) {
        if ("local-stub".equalsIgnoreCase(provider)) {
            return localStubClient.generate(request, apiKeyOverride);
        }
        return openAiClient.generate(request, apiKeyOverride);
    }
}
