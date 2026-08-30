package com.aditya.roleplay.llm.openai;

import com.aditya.roleplay.exception.LlmException;
import com.aditya.roleplay.llm.LlmClient;
import com.aditya.roleplay.llm.LlmMessage;
import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.llm.LlmResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class OpenAiLlmClient implements LlmClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ConfigProperty(name = "roleplay.llm.openai.api-key")
    String apiKey;

    @ConfigProperty(name = "roleplay.llm.openai.model")
    String model;

    @ConfigProperty(name = "roleplay.llm.openai.base-url")
    String baseUrl;

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmException("OpenAI API key is not configured. Set OPENAI_API_KEY in backend/.env");
        }

        try {
            return callWithRetry(request, false);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Failed to call OpenAI: " + e.getMessage(), e);
        }
    }

    private LlmResponse callWithRetry(LlmRequest request, boolean isRetry) throws Exception {
        List<Map<String, String>> apiMessages = new ArrayList<>();
        apiMessages.add(Map.of("role", "system", "content", request.systemPrompt()));
        for (LlmMessage message : request.messages()) {
            apiMessages.add(Map.of("role", message.role(), "content", message.content()));
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", apiMessages,
                "temperature", request.temperature(),
                "max_tokens", request.maxTokens());

        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if ((response.statusCode() == 429 || response.statusCode() >= 500) && !isRetry) {
            Thread.sleep(1000);
            return callWithRetry(request, true);
        }

        if (response.statusCode() != 200) {
            throw new LlmException("OpenAI returned status " + response.statusCode() + ": " + response.body());
        }

        ChatCompletionResponse parsed = objectMapper.readValue(response.body(), ChatCompletionResponse.class);
        if (parsed.choices == null || parsed.choices.isEmpty()) {
            throw new LlmException("OpenAI returned no choices");
        }

        String content = parsed.choices.get(0).message.content;
        Integer tokens = parsed.usage != null ? parsed.usage.totalTokens : null;
        return new LlmResponse(content, parsed.model != null ? parsed.model : model, tokens);
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
