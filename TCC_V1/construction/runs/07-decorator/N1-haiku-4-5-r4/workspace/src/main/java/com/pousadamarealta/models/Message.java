package com.pousadamarealta.models;

public record Message(
    int id,
    MessageChannel channel,
    String to,
    String subject,
    String body,
    MessageStatus status
) {}
