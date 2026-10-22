package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageChannel;

public record PendingMessage(
        MessageChannel channel,
        String to,
        String subject,
        String body
) {
}
