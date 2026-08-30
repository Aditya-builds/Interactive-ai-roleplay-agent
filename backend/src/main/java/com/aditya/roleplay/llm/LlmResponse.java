package com.aditya.roleplay.llm;

public record LlmResponse(
        String content,
        String model,
        Integer tokenUsage) {
}
