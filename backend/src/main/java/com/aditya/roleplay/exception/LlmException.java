package com.aditya.roleplay.exception;

public class LlmException extends RoleplayException {

    public LlmException(String message) {
        super(message, "LLM_ERROR", 502);
    }

    public LlmException(String message, Throwable cause) {
        super(message, "LLM_ERROR", 502);
        initCause(cause);
    }
}
