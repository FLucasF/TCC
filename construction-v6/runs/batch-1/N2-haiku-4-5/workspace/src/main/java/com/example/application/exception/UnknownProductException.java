package com.example.application.exception;

public class UnknownProductException extends ApplicationException {
    public UnknownProductException() {
        super("UNKNOWN_PRODUCT", 422);
    }
}
