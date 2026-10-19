package com.pousadamarealta.exception;

public class ApiException extends RuntimeException {
    private final String errorCode;
    private final int statusCode;

    public ApiException(String errorCode, String message, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
