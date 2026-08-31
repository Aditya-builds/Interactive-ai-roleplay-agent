package com.aditya.roleplay.visual.openai;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.visual.ImageGenerationRequest;
import com.aditya.roleplay.visual.ImageGenerationResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class OpenAiCompatibleImageGenerationClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ConfigProperty(name = "roleplay.llm.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "roleplay.llm.openai.api-key")
    Optional<String> legacyOpenAiApiKey;

    @ConfigProperty(name = "roleplay.llm.openai.base-url")
    Optional<String> legacyOpenAiBaseUrl;

    @ConfigProperty(name = "roleplay.visual.base-url")
    Optional<String> visualBaseUrl;

    @ConfigProperty(name = "roleplay.visual.model", defaultValue = "gpt-image-2")
    String defaultModel;

    @ConfigProperty(name = "roleplay.visual.provider", defaultValue = "openai")
    String providerName;

    public ImageGenerationResponse generate(ImageGenerationRequest request) {
        return generate(request, null);
    }

    public ImageGenerationResponse generate(ImageGenerationRequest request, String apiKeyOverride) {
        String resolvedApiKey = resolveApiKey(apiKeyOverride);
        if (resolvedApiKey.isBlank()) {
            throw new RoleplayException(
                    "Image API key is not configured. Enter your key in the app settings or set LLM_API_KEY / OPENAI_API_KEY on the server.",
                    "VISUAL_API_KEY_MISSING",
                    400);
        }

        try {
            return callImageApi(request, resolvedApiKey);
        } catch (RoleplayException e) {
            throw e;
        } catch (Exception e) {
            throw new RoleplayException("Failed to generate scene image: " + e.getMessage(), "VISUAL_GENERATION_ERROR", 500);
        }
    }

    private ImageGenerationResponse callImageApi(ImageGenerationRequest request, String resolvedApiKey) throws Exception {
        String resolvedBaseUrl = visualBaseUrl
                .or(() -> legacyOpenAiBaseUrl)
                .orElse("https://api.openai.com/v1");
        if (resolvedBaseUrl.endsWith("/")) {
            resolvedBaseUrl = resolvedBaseUrl.substring(0, resolvedBaseUrl.length() - 1);
        }

        String model = request.model() != null && !request.model().isBlank() && !"local-stub".equals(request.model())
                ? request.model()
                : defaultModel;

        String prompt = request.prompt();
        if (request.negativePrompt() != null && !request.negativePrompt().isBlank()) {
            prompt = prompt + "\n\nAvoid: " + request.negativePrompt();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("n", 1);
        body.put("size", mapSize(request.width(), request.height(), model));
        if (isGptImageModel(model)) {
            body.put("output_format", "png");
            body.put("quality", "medium");
        }
        if (supportsResponseFormat(model)) {
            body.put("response_format", "b64_json");
        }

        HttpResponse<String> response = postImageGeneration(resolvedBaseUrl, resolvedApiKey, body);
        if (response.statusCode() == 400
                && response.body() != null
                && response.body().contains("response_format")
                && body.containsKey("response_format")) {
            body.remove("response_format");
            response = postImageGeneration(resolvedBaseUrl, resolvedApiKey, body);
        }

        if (response.statusCode() != 200) {
            throw new RoleplayException(
                    "Image API error (" + response.statusCode() + "): " + response.body(),
                    "VISUAL_GENERATION_ERROR",
                    response.statusCode() >= 500 ? 502 : 400);
        }

        ImageApiResponse parsed = objectMapper.readValue(response.body(), ImageApiResponse.class);
        if (parsed.data == null || parsed.data.isEmpty()) {
            throw new RoleplayException("Image API returned no image data", "VISUAL_GENERATION_ERROR", 502);
        }

        ImageData imageData = parsed.data.get(0);
        byte[] imageBytes;
        if (imageData.b64_json != null && !imageData.b64_json.isBlank()) {
            imageBytes = Base64.getDecoder().decode(imageData.b64_json);
        } else if (imageData.url != null && !imageData.url.isBlank()) {
            imageBytes = downloadImage(imageData.url);
        } else {
            throw new RoleplayException("Image API returned no image data", "VISUAL_GENERATION_ERROR", 502);
        }

        return new ImageGenerationResponse(imageBytes, "image/png", providerName, model);
    }

    private HttpResponse<String> postImageGeneration(String baseUrl, String apiKey, Map<String, Object> body)
            throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/images/generations"))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private byte[] downloadImage(String imageUrl) throws Exception {
        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();
        HttpResponse<byte[]> downloadResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (downloadResponse.statusCode() != 200) {
            throw new RoleplayException(
                    "Failed to download generated image (" + downloadResponse.statusCode() + ")",
                    "VISUAL_GENERATION_ERROR",
                    502);
        }
        return downloadResponse.body();
    }

    private static boolean isGptImageModel(String model) {
        return model != null && model.toLowerCase().startsWith("gpt-image");
    }

    private static boolean supportsResponseFormat(String model) {
        if (model == null || model.isBlank()) {
            return false;
        }
        String normalized = model.toLowerCase();
        return normalized.startsWith("dall-e-");
    }

    private static String mapSize(int width, int height, String model) {
        if (isGptImageModel(model)) {
            if (width > height) {
                return "1536x864";
            }
            if (height > width) {
                return "864x1536";
            }
            return "1024x1024";
        }
        if (width > height) {
            return "1792x1024";
        }
        if (height > width) {
            return "1024x1792";
        }
        return "1024x1024";
    }

    private String resolveApiKey(String apiKeyOverride) {
        if (apiKeyOverride != null && !apiKeyOverride.isBlank()) {
            return apiKeyOverride.trim();
        }
        return apiKey.or(() -> legacyOpenAiApiKey).orElse("").trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ImageApiResponse {
        public List<ImageData> data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ImageData {
        public String b64_json;
        public String url;
    }
}
