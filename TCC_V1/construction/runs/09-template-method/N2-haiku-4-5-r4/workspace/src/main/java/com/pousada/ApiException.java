package com.pousada;

import io.javalin.http.HttpStatus;

public class ApiException extends RuntimeException {
    public HttpStatus statusCode;
    public int statusCodeNumber;
    public String errorCode;
    public String message;

    public ApiException(HttpStatus statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.statusCodeNumber = statusCode.getCode();
        this.errorCode = errorCode;
        this.message = message;
    }

    public ApiException(int statusCodeNumber, String errorCode, String message) {
        super(message);
        this.statusCodeNumber = statusCodeNumber;
        this.errorCode = errorCode;
        this.message = message;
    }
}
