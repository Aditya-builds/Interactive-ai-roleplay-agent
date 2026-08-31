package com.aditya.roleplay.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    public NarrativeParseResult parseNarrative(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return NarrativeParseResult.failure("LLM returned empty content");
        }

        String json = extractJson(rawContent.trim());
        try {
            NarrativePayload payload = objectMapper.readValue(json, NarrativePayload.class);
            if (payload.response == null || payload.response.isBlank()) {
                return NarrativeParseResult.failure("Narrative response missing text");
            }
            return NarrativeParseResult.success(payload.response.trim());
        } catch (Exception e) {
            return NarrativeParseResult.failure("Failed to parse narrative output: " + e.getMessage());
        }
    }

    public StateExtractionParseResult parseStateExtraction(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return StateExtractionParseResult.failure("LLM returned empty content");
        }

        String json = extractJson(rawContent.trim());
        try {
            LlmStateExtraction extraction = objectMapper.readValue(json, LlmStateExtraction.class);
            return StateExtractionParseResult.success(extraction);
        } catch (Exception e) {
            return StateExtractionParseResult.failure("Failed to parse state extraction output: " + e.getMessage());
        }
    }

    private String extractJson(String content) {
        Matcher matcher = JSON_BLOCK.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NarrativePayload {
        public String response;
    }

    public record ParseResult(boolean success, LlmTurnResult turnResult, String errorMessage) {
        public static ParseResult success(LlmTurnResult turnResult) {
            return new ParseResult(true, turnResult, null);
        }

        public static ParseResult failure(String errorMessage) {
            return new ParseResult(false, null, errorMessage);
        }
    }

    public record NarrativeParseResult(boolean success, String narrative, String errorMessage) {
        public static NarrativeParseResult success(String narrative) {
            return new NarrativeParseResult(true, narrative, null);
        }

        public static NarrativeParseResult failure(String errorMessage) {
            return new NarrativeParseResult(false, null, errorMessage);
        }
    }

    public record StateExtractionParseResult(boolean success, LlmStateExtraction extraction, String errorMessage) {
        public static StateExtractionParseResult success(LlmStateExtraction extraction) {
            return new StateExtractionParseResult(true, extraction, null);
        }

        public static StateExtractionParseResult failure(String errorMessage) {
            return new StateExtractionParseResult(false, null, errorMessage);
        }
    }
}
