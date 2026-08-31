package com.aditya.roleplay.llm.openai;

import com.aditya.roleplay.exception.LlmException;
import com.aditya.roleplay.llm.LlmClient;
import com.aditya.roleplay.llm.LlmMessage;
import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.llm.LlmRequestKind;
import com.aditya.roleplay.llm.LlmResponse;
import com.aditya.roleplay.llm.LlmTurnResult;
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

    @ConfigProperty(name = "roleplay.llm.max-retries")
    int maxRetries;

    @ConfigProperty(name = "roleplay.llm.retry-base-delay-ms")
    long retryBaseDelayMs;

    @Override
    public LlmResponse complete(LlmRequest request) {
        return complete(request, null);
    }

    @Override
    public LlmResponse complete(LlmRequest request, String apiKeyOverride) {
        String resolvedApiKey = resolveApiKey(apiKeyOverride);
        if (resolvedApiKey.isBlank()) {
            throw new LlmException(
                    "LLM API key is not configured. Enter your key in the app settings or set LLM_API_KEY / OPENAI_API_KEY on the server.");
        }

        try {
            return executeAttempt(request, resolvedApiKey, 0);
        } catch (LlmException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("LLM request interrupted during retry backoff", e);
        } catch (Exception e) {
            throw new LlmException("Failed to call LLM: " + e.getMessage(), e);
        }
    }

    private String resolveApiKey(String apiKeyOverride) {
        if (apiKeyOverride != null && !apiKeyOverride.isBlank()) {
            return apiKeyOverride.trim();
        }
        return apiKey.or(() -> legacyOpenAiApiKey).orElse("").trim();
    }

    private LlmResponse executeAttempt(LlmRequest request, String resolvedApiKey, int retryCount)
            throws Exception {
        HttpResponse<String> response = sendRequest(request, resolvedApiKey);
        int status = response.statusCode();
        String body = response.body();

        if (LlmRetryPolicy.isJsonModeUnsupported(status, body, request.jsonMode())) {
            LlmRequest fallbackRequest = new LlmRequest(
                    request.systemPrompt(),
                    request.messages(),
                    request.temperature(),
                    request.maxTokens(),
                    false,
                    request.kind());
            return executeAttempt(fallbackRequest, resolvedApiKey, retryCount);
        }

        if (LlmRetryPolicy.isRetryable(status) && retryCount < maxRetries) {
            Thread.sleep(LlmRetryPolicy.backoffDelayMs(retryCount, retryBaseDelayMs));
            return executeAttempt(request, resolvedApiKey, retryCount + 1);
        }

        if (status != 200) {
            throw new LlmException(LlmRetryPolicy.httpErrorMessage(status, body));
        }

        return parseSuccessResponse(body, request);
    }

    private HttpResponse<String> sendRequest(LlmRequest request, String resolvedApiKey) throws Exception {
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

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", resolvedModel);
        requestBody.put("messages", apiMessages);
        requestBody.put("temperature", request.temperature());
        requestBody.put("max_tokens", request.maxTokens());
        if (request.jsonMode()) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(resolvedBaseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + resolvedApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private LlmResponse parseSuccessResponse(String body, LlmRequest request)
            throws Exception {
        ChatCompletionResponse parsed = objectMapper.readValue(body, ChatCompletionResponse.class);
        if (parsed.choices == null || parsed.choices.isEmpty()) {
            throw new LlmException("LLM returned no choices");
        }

        Choice firstChoice = parsed.choices.get(0);
        if (firstChoice.message == null || firstChoice.message.content == null || firstChoice.message.content.isBlank()) {
            throw new LlmException("LLM returned an empty message");
        }

        String content = firstChoice.message.content;
        Integer tokens = parsed.usage != null ? parsed.usage.totalTokens : null;
        String responseModel = parsed.model != null ? parsed.model : model.or(() -> legacyOpenAiModel).orElse("gpt-4");

        return switch (request.kind()) {
            case NARRATIVE_ONLY -> {
                LlmTurnResultParser.NarrativeParseResult parseResult = turnResultParser.parseNarrative(content);
                LlmTurnResult turnResult = parseResult.success()
                        ? new LlmTurnResult(parseResult.narrative(), List.of(), List.of(), List.of())
                        : null;
                yield new LlmResponse(content, turnResult, responseModel, tokens, parseResult.success());
            }
            case STATE_EXTRACTION -> {
                LlmTurnResultParser.StateExtractionParseResult parseResult =
                        turnResultParser.parseStateExtraction(content);
                LlmTurnResult turnResult = parseResult.success()
                        ? new LlmTurnResult(
                                "",
                                parseResult.extraction().stateChanges(),
                                parseResult.extraction().events(),
                                parseResult.extraction().memories())
                        : null;
                yield new LlmResponse(content, turnResult, responseModel, tokens, parseResult.success());
            }
            case FULL_TURN -> {
                LlmTurnResultParser.ParseResult parseResult = turnResultParser.parse(content);
                yield new LlmResponse(
                        content, parseResult.turnResult(), responseModel, tokens, parseResult.success());
            }
        };
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
