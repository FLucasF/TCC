package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutboundMessage;
import com.pousadamarealta.messaging.RuleContext;

public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_RECIPIENT = "arquivo@pousadamarealta.com.br";

    @Override
    public void apply(OutboundMessage message, RuleContext context) {
        if (message.isBlocked() || context.isTestMode()) {
            return;
        }

        OutboundMessage archiveCopy = new OutboundMessage(
            message.getChannel(),
            ARCHIVE_RECIPIENT,
            "[Cópia] " + message.getSubject(),
            message.getBody()
        );

        context.addMessage(archiveCopy);
    }
}
