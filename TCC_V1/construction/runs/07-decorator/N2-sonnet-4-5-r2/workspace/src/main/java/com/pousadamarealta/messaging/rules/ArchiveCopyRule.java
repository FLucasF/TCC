package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutgoingMessage;

public final class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_RECIPIENT = "arquivo@pousadamarealta.com.br";
    private static final String COPY_PREFIX = "[Cópia] ";

    @Override
    public boolean apply(OutgoingMessage message) {
        if (message.isBlocked()) {
            return true;
        }

        OutgoingMessage copy = new OutgoingMessage(
            message.getChannel(),
            ARCHIVE_RECIPIENT,
            COPY_PREFIX + message.getSubject(),
            message.getBody()
        );

        message.addAdditionalMessage(copy);
        return true;
    }
}
