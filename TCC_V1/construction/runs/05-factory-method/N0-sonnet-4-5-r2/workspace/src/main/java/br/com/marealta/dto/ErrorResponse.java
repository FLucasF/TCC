package br.com.marealta.dto;

public record ErrorResponse(
    String error,
    String message
) {
}
