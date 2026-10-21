package com.pousadamarealta.domain;

public record Message(
    int id,
    MessageChannel channel,
    String to,
    String subject,
    String body
) {}
