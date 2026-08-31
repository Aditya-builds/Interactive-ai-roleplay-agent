package com.aditya.roleplay.llm.openai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmRetryPolicyTest {

    @Test
    void identifiesRetryableStatuses() {
        assertTrue(LlmRetryPolicy.isRetryable(429));
        assertTrue(LlmRetryPolicy.isRetryable(500));
        assertTrue(LlmRetryPolicy.isRetryable(503));
        assertFalse(LlmRetryPolicy.isRetryable(401));
        assertFalse(LlmRetryPolicy.isRetryable(400));
    }

    @Test
    void identifiesAuthFailures() {
        assertTrue(LlmRetryPolicy.isAuthFailure(401));
        assertTrue(LlmRetryPolicy.isAuthFailure(403));
        assertFalse(LlmRetryPolicy.isAuthFailure(429));
    }

    @Test
    void exponentialBackoff() {
        assertEquals(1000L, LlmRetryPolicy.backoffDelayMs(0, 1000));
        assertEquals(2000L, LlmRetryPolicy.backoffDelayMs(1, 1000));
        assertEquals(4000L, LlmRetryPolicy.backoffDelayMs(2, 1000));
    }

    @Test
    void authErrorMessageIsClear() {
        String message = LlmRetryPolicy.httpErrorMessage(401, "{\"error\":\"invalid_api_key\"}");
        assertEquals("Invalid LLM API key. Check your key in app settings or server configuration.", message);
    }

    @Test
    void rateLimitErrorMessageIsDistinctFromServerError() {
        assertEquals(
                "LLM rate limit reached. Please wait a moment and try again.",
                LlmRetryPolicy.httpErrorMessage(429, "rate limit"));
        assertEquals(
                "LLM provider is temporarily unavailable. Please try again shortly.",
                LlmRetryPolicy.httpErrorMessage(503, "overloaded"));
    }

    @Test
    void detectsJsonModeUnsupported() {
        assertTrue(LlmRetryPolicy.isJsonModeUnsupported(
                400, "response_format is not supported", true));
        assertFalse(LlmRetryPolicy.isJsonModeUnsupported(
                400, "response_format is not supported", false));
    }
}
