package com.jets.backend.core.service;

public class LobbyException extends RuntimeException {

    private final ErrorCode errorCode;

    public LobbyException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
