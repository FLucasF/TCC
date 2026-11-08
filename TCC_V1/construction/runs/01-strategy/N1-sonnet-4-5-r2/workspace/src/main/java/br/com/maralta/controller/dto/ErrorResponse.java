package br.com.maralta.controller.dto;

public record ErrorResponse(
    String error,
    String message
) {
}
