package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;

public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String COPY_PREFIX = "[Cópia] ";

    @Override
    public void apply(MessageContext context) {
        Message original = context.getMessage();

        if (original.getStatus() == MessageStatus.BLOCKED) {
            return;
        }

        Message copy = new Message(
            original.getChannel(),
            ARCHIVE_EMAIL,
            COPY_PREFIX + original.getSubject(),
            original.getBody()
        );

        context.addMessage(copy);
    }
}
