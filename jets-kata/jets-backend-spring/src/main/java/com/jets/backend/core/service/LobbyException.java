package com.jets.backend.core.service;

public class LobbyException extends RuntimeException {

    private final String errorCode;

    public LobbyException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
