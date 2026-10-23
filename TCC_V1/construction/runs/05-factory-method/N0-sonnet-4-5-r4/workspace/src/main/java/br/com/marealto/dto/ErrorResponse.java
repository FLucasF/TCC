package br.com.marealto.dto;

public record ErrorResponse(
        String error,
        String message
) {
}
