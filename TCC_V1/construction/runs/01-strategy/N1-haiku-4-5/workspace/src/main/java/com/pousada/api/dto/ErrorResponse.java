package com.pousada.api.dto;

public class ErrorResponse {
    public String error;
    public String message;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
}
