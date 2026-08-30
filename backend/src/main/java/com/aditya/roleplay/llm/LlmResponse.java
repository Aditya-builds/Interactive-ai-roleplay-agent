package com.aditya.roleplay.llm;

public record LlmResponse(
        String rawContent,
        LlmTurnResult turnResult,
        String model,
        Integer tokenUsage,
        boolean structuredParseSuccess) {

    public String narrativeResponse() {
        if (structuredParseSuccess && turnResult != null && turnResult.response() != null && !turnResult.response().isBlank()) {
            return turnResult.response().trim();
        }
        return rawContent != null ? rawContent.trim() : "";
    }
}
