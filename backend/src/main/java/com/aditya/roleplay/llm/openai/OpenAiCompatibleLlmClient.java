package com.aditya.roleplay.llm.openai;

import com.aditya.roleplay.exception.LlmException;
import com.aditya.roleplay.llm.LlmClient;
import com.aditya.roleplay.llm.LlmMessage;
import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.llm.LlmResponse;
import com.aditya.roleplay.llm.LlmTurnResultParser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    LlmTurnResultParser turnResultParser;

    @ConfigProperty(name = "roleplay.llm.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "roleplay.llm.openai.api-key")
    Optional<String> legacyOpenAiApiKey;

    @ConfigProperty(name = "roleplay.llm.model")
    Optional<String> model;

    @ConfigProperty(name = "roleplay.llm.openai.model")
    Optional<String> legacyOpenAiModel;

    @ConfigProperty(name = "roleplay.llm.base-url")
    Optional<String> baseUrl;

    @ConfigProperty(name = "roleplay.llm.openai.base-url")
    Optional<String> legacyOpenAiBaseUrl;

    @Override
    public LlmResponse complete(LlmRequest request) {
        String resolvedApiKey = apiKey.or(() -> legacyOpenAiApiKey).orElse("").trim();
        if (resolvedApiKey.isBlank()) {
            throw new LlmException("LLM API key is not configured. Set LLM_API_KEY or OPENAI_API_KEY in backend/.env");
        }

        try {
            return callWithRetry(request, resolvedApiKey, false);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Failed to call LLM: " + e.getMessage(), e);
        }
    }

    private LlmResponse callWithRetry(LlmRequest request, String resolvedApiKey, boolean isRetry) throws Exception {
        String resolvedModel = model.or(() -> legacyOpenAiModel).orElse("gpt-4");
        String resolvedBaseUrl = baseUrl.or(() -> legacyOpenAiBaseUrl).orElse("https://api.openai.com/v1");
        if (resolvedBaseUrl.endsWith("/")) {
            resolvedBaseUrl = resolvedBaseUrl.substring(0, resolvedBaseUrl.length() - 1);
        }

        List<Map<String, String>> apiMessages = new ArrayList<>();
        apiMessages.add(Map.of("role", "system", "content", request.systemPrompt()));
        for (LlmMessage message : request.messages()) {
            apiMessages.add(Map.of("role", message.role(), "content", message.content()));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", resolvedModel);
        body.put("messages", apiMessages);
        body.put("temperature", request.temperature());
        body.put("max_tokens", request.maxTokens());
        if (request.jsonMode()) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(resolvedBaseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + resolvedApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 400 && request.jsonMode() && response.body().contains("response_format") && !isRetry) {
            LlmRequest fallbackRequest = new LlmRequest(
                    request.systemPrompt(),
                    request.messages(),
                    request.temperature(),
                    request.maxTokens(),
                    false);
            return callWithRetry(fallbackRequest, resolvedApiKey, true);
        }

        if ((response.statusCode() == 429 || response.statusCode() >= 500) && !isRetry) {
            Thread.sleep(1000);
            return callWithRetry(request, resolvedApiKey, true);
        }

        if (response.statusCode() != 200) {
            throw new LlmException("LLM returned status " + response.statusCode() + ": " + response.body());
        }

        ChatCompletionResponse parsed = objectMapper.readValue(response.body(), ChatCompletionResponse.class);
        if (parsed.choices == null || parsed.choices.isEmpty()) {
            throw new LlmException("LLM returned no choices");
        }

        String content = parsed.choices.get(0).message.content;
        Integer tokens = parsed.usage != null ? parsed.usage.totalTokens : null;
        String responseModel = parsed.model != null ? parsed.model : resolvedModel;

        if (request.jsonMode()) {
            LlmTurnResultParser.ParseResult parseResult = turnResultParser.parse(content);
            return new LlmResponse(
                    content,
                    parseResult.turnResult(),
                    responseModel,
                    tokens,
                    parseResult.success());
        }

        LlmTurnResultParser.ParseResult parseResult = turnResultParser.parse(content);
        if (parseResult.success()) {
            return new LlmResponse(content, parseResult.turnResult(), responseModel, tokens, true);
        }

        return new LlmResponse(content, null, responseModel, tokens, false);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChatCompletionResponse {
        public String model;
        public List<Choice> choices;
        public Usage usage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Choice {
        public Message message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Message {
        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Usage {
        public Integer totalTokens;
    }
}
