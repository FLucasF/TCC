package com.pousadamareaita.http.dto;

public class ErrorDto {
    public String error;
    public String message;

    public ErrorDto(String error, String message) {
        this.error = error;
        this.message = message;
    }
}
