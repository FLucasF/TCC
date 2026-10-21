package com.pousada.dto;

public class ErrorDTO {
    public String error;
    public String message;

    public ErrorDTO(String error, String message) {
        this.error = error;
        this.message = message;
    }
}
