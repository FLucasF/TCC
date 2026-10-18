package com.example.application.exception;

public class InvalidRegionException extends ApplicationException {
    public InvalidRegionException() {
        super("INVALID_REGION", 422);
    }
}
