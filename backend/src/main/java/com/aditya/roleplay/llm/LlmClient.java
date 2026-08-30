package com.aditya.roleplay.llm;

public interface LlmClient {
    LlmResponse complete(LlmRequest request);

    default LlmResponse complete(LlmRequest request, String apiKeyOverride) {
        return complete(request);
    }
}
