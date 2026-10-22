package com.pousadamarealta.api.dto;

public record ErrorResponse(
    String error,
    String message
) {
}
