package com.pousadamarealta.dto;

public record ErrorResponse(
    String error,
    String message
) {}
