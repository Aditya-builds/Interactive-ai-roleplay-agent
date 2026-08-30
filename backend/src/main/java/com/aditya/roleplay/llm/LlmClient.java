package com.aditya.roleplay.llm;

public interface LlmClient {
    LlmResponse complete(LlmRequest request);
}
