package com.chatapp.exception;

public class WebSocketSecurityException extends RuntimeException {
    private final String code;

    public WebSocketSecurityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
