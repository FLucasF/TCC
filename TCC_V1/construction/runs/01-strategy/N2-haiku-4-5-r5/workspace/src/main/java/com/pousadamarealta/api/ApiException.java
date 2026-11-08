package com.pousadamarealta.api;

public class ApiException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;
    private final String errorMessage;

    public ApiException(int statusCode, String errorCode, String errorMessage) {
        super(errorMessage);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
