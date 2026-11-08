package com.pousadamarealta.dto;

public record MessageResponse(
    long id,
    String channel,
    String to,
    String subject,
    String body
) {
}
