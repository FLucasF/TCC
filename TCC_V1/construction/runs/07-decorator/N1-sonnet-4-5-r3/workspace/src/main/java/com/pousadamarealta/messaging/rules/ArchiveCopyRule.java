package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageContext;
import com.pousadamarealta.messaging.MessageRequest;
import com.pousadamarealta.messaging.MessageRule;

public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String COPY_PREFIX = "[Cópia] ";

    @Override
    public MessageContext apply(MessageContext context) {
        MessageRequest current = context.getCurrent();

        String copySubject = COPY_PREFIX + current.getSubject();
        MessageRequest copy = current
                .withTo(ARCHIVE_EMAIL)
                .withSubject(copySubject);

        return context.withAdditional(copy);
    }
}
