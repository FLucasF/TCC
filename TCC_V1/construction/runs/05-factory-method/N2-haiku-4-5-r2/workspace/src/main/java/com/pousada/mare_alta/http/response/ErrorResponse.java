package com.pousada.mare_alta.http.response;

public class ErrorResponse {
    public String error;
    public String message;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
}
