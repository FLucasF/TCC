package com.pousadamarealta.dto;

public class ErrorResponse {
    public final String error;
    public final String message;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
}
