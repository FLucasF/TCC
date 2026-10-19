package com.pousadamarealta.domain;

public record Message(
    long id,
    MessageChannel channel,
    String to,
    String subject,
    String body
) {
}
