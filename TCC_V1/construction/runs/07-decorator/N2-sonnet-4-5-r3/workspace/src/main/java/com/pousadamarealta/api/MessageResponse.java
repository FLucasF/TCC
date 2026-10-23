package com.pousadamarealta.api;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.SentMessage;
import com.pousadamarealta.messaging.MessageStatus;

public record MessageResponse(
    Long id,
    MessageChannel channel,
    String to,
    String subject,
    String body,
    MessageStatus status
) {
    public static MessageResponse from(SentMessage message) {
        return new MessageResponse(
            message.id(),
            message.channel(),
            message.to(),
            message.subject(),
            message.body(),
            message.status()
        );
    }
}
