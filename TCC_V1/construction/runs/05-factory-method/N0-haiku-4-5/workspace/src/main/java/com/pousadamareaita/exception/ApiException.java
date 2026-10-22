package com.pousadamareaita.exception;

public class ApiException extends RuntimeException {
    private final String code;
    private final int statusCode;

    public ApiException(String code, String message, int statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public String getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
