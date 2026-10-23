package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.messaging.ProcessingResult;

public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String COPY_PREFIX = "[Cópia] ";

    @Override
    public ProcessingResult process(OutgoingMessage message) {
        ProcessingResult result = ProcessingResult.allow(message);

        OutgoingMessage copy = message.copy();
        copy.setTo(ARCHIVE_EMAIL);
        copy.setSubject(COPY_PREFIX + message.getSubject());

        result.addAdditionalMessage(copy);

        return result;
    }
}
