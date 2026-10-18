package com.example.application.exception;

public class InvalidOrderException extends ApplicationException {
    public InvalidOrderException() {
        super("INVALID_ORDER", 422);
    }
}
