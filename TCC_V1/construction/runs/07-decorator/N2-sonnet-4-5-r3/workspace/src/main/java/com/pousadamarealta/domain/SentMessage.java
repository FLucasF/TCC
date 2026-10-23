package com.pousadamarealta.domain;

import com.pousadamarealta.messaging.MessageStatus;

public record SentMessage(
    Long id,
    MessageChannel channel,
    String to,
    String subject,
    String body,
    MessageStatus status
) {}
