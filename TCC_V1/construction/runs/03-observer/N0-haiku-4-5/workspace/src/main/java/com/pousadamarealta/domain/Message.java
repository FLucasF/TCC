package com.pousadamarealta.domain;

public record Message(
    long id,
    String channel,
    String to,
    String subject,
    String body
) {}
