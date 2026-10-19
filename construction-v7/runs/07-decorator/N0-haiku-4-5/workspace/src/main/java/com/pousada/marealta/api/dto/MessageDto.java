package com.pousada.marealta.api.dto;

public record MessageDto(
    long id,
    String channel,
    String to,
    String subject,
    String body,
    String status
) {}
