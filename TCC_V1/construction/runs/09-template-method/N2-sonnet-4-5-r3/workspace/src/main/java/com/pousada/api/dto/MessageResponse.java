package com.pousada.api.dto;

public record MessageResponse(
    Long id,
    String channel,
    String to,
    String subject,
    String body
) {
}
