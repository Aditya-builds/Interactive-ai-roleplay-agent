package com.aditya.roleplay.exception;

public class RoleplayException extends RuntimeException {

    private final String code;
    private final int status;

    public RoleplayException(String message, String code, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
