package com.pousadamarealta.service;

public class ServiceException extends RuntimeException {
    private final String errorCode;
    private final int statusCode;

    public ServiceException(String errorCode, String message, int statusCode) {
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
