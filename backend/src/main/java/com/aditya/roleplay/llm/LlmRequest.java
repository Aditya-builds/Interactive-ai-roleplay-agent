package com.aditya.roleplay.llm;

import java.util.List;

public record LlmRequest(
        String systemPrompt,
        List<LlmMessage> messages,
        double temperature,
        int maxTokens) {
}
