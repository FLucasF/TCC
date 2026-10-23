package com.marealta.api.dto;

public record ErrorResponse(
    String error,
    String message
) {
}
