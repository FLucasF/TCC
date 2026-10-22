package com.pousadamarealta.messaging;

public record OutgoingMessage(
    MessageChannel channel,
    String to,
    String subject,
    String body
) {}
