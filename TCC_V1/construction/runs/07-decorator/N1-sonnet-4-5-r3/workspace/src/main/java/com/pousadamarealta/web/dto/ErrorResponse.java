package com.pousadamarealta.web.dto;

public record ErrorResponse(
    String error,
    String message
) {}
