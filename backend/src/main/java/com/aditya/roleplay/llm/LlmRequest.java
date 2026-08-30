package com.aditya.roleplay.llm;

import java.util.List;

public record LlmRequest(
        String systemPrompt,
        List<LlmMessage> messages,
        double temperature,
        int maxTokens,
        boolean jsonMode) {

    public LlmRequest(String systemPrompt, List<LlmMessage> messages, double temperature, int maxTokens) {
        this(systemPrompt, messages, temperature, maxTokens, false);
    }
}
