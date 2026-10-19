package com.pousadamarealta.api.dto;

public record MessageDTO(
    long id,
    String channel,
    String to,
    String subject,
    String body
) {}
