package com.pousada.marealta.api.dto;

public record ErrorResponse(
    String error,
    String message
) {}
