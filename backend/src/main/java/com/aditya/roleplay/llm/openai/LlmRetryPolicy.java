package com.aditya.roleplay.llm.openai;

final class LlmRetryPolicy {

    private LlmRetryPolicy() {
    }

    static boolean isRetryable(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    static boolean isAuthFailure(int statusCode) {
        return statusCode == 401 || statusCode == 403;
    }

    static boolean isJsonModeUnsupported(int statusCode, String body, boolean jsonMode) {
        return statusCode == 400
                && jsonMode
                && body != null
                && body.contains("response_format");
    }

    static long backoffDelayMs(int retryAttempt, long baseDelayMs) {
        if (retryAttempt <= 0) {
            return baseDelayMs;
        }
        return baseDelayMs * (1L << retryAttempt);
    }

    static String httpErrorMessage(int statusCode, String body) {
        if (isAuthFailure(statusCode)) {
            return "Invalid LLM API key. Check your key in app settings or server configuration.";
        }
        if (statusCode == 429) {
            return "LLM rate limit reached. Please wait a moment and try again.";
        }
        if (statusCode >= 500) {
            return "LLM provider is temporarily unavailable. Please try again shortly.";
        }
        return "LLM returned status " + statusCode + ": " + truncate(body, 300);
    }

    private static String truncate(String body, int maxLength) {
        if (body == null || body.isBlank()) {
            return "";
        }
        if (body.length() <= maxLength) {
            return body;
        }
        return body.substring(0, maxLength).trim() + "…";
    }
}
