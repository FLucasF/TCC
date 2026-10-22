package com.pousadamarealta;

public class ApiException extends RuntimeException {
    public final String errorCode;
    public final int statusCode;

    public ApiException(String errorCode, String message, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
}
