package com.pousadamarealta.exception;

public class InvalidRequestException extends ApiException {
    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", message, 400);
    }
}
