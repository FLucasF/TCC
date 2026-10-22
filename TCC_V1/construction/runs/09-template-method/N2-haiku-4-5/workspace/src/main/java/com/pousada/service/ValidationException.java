package com.pousada.service;

public class ValidationException extends Exception {
    private final String errorCode;

    public ValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
