package com.aditya.roleplay.llm;

import java.util.List;

public record LlmRequest(
        String systemPrompt,
        List<LlmMessage> messages,
        double temperature,
        int maxTokens,
        boolean jsonMode,
        LlmRequestKind kind) {

    public LlmRequest(String systemPrompt, List<LlmMessage> messages, double temperature, int maxTokens) {
        this(systemPrompt, messages, temperature, maxTokens, false, LlmRequestKind.FULL_TURN);
    }

    public LlmRequest(String systemPrompt, List<LlmMessage> messages, double temperature, int maxTokens, boolean jsonMode) {
        this(systemPrompt, messages, temperature, maxTokens, jsonMode, LlmRequestKind.FULL_TURN);
    }
}
