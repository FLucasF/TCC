package com.example.application.exception;

public class InvalidZipCodeException extends ApplicationException {
    public InvalidZipCodeException() {
        super("INVALID_ZIP_CODE", 422);
    }
}
