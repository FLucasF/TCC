package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.MessageRuleContext;

public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    @Override
    public void apply(MessageRuleContext context) {
        if (context.getStatus() == MessageStatus.SENT) {
            String archiveSubject = "[Cópia] " + context.getSubject();
            context.addAdditionalMessage(ARCHIVE_EMAIL, archiveSubject, context.getBody());
        }
    }
}
