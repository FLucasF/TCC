package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageStatus;

public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    @Override
    public void apply(ProcessedMessage message) {
        if (message.getStatus() != MessageStatus.SENT) {
            return;
        }

        ProcessedMessage copy = new ProcessedMessage(
                new PendingMessage(
                        message.getChannel(),
                        ARCHIVE_EMAIL,
                        "[Cópia] " + message.getSubject(),
                        message.getBody()
                )
        );

        message.addAdditionalMessage(copy);
    }
}
