package com.aditya.roleplay.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class LlmTurnResultParser {

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParseResult parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return ParseResult.failure("LLM returned empty content");
        }

        String json = extractJson(rawContent.trim());
        try {
            LlmTurnResult result = objectMapper.readValue(json, LlmTurnResult.class);
            if (result.response() == null || result.response().isBlank()) {
                return ParseResult.failure("Structured response missing narrative text");
            }
            return ParseResult.success(result);
        } catch (Exception e) {
            return ParseResult.failure("Failed to parse structured LLM output: " + e.getMessage());
        }
    }

    private String extractJson(String content) {
        Matcher matcher = JSON_BLOCK.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return content;
    }

    public record ParseResult(boolean success, LlmTurnResult turnResult, String errorMessage) {
        public static ParseResult success(LlmTurnResult turnResult) {
            return new ParseResult(true, turnResult, null);
        }

        public static ParseResult failure(String errorMessage) {
            return new ParseResult(false, null, errorMessage);
        }
    }
}
