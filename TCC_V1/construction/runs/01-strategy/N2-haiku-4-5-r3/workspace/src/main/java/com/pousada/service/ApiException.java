package com.pousada.service;

public class ApiException extends Exception {
    public final String code;
    public final int statusCode;

    public ApiException(String code, String message, int statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }
}
