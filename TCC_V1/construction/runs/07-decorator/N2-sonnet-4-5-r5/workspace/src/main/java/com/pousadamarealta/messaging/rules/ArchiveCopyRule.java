package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageContext;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutgoingMessage;

public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String SUBJECT_PREFIX = "[Cópia] ";

    @Override
    public void process(MessageContext context) {
        if (!context.isBlocked()) {
            OutgoingMessage original = context.getMessage();
            OutgoingMessage copy = original.copy(
                ARCHIVE_EMAIL,
                SUBJECT_PREFIX + original.getSubject(),
                original.getBody()
            );
            context.addMessage(copy);
        }
    }
}
