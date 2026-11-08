package com.pousadamarealta.exceptions;

public class ApiException extends Exception {
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
