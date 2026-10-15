package com.loja.service;

public class ValidationException extends Exception {
    private String errorCode;

    public ValidationException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
