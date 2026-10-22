package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.messaging.ProcessingResult;

import java.util.List;

public final class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String SUBJECT_PREFIX = "[Cópia] ";

    @Override
    public ProcessingResult process(OutgoingMessage message) {
        OutgoingMessage archiveCopy = new OutgoingMessage(
            message.channel(),
            ARCHIVE_EMAIL,
            SUBJECT_PREFIX + message.subject(),
            message.body()
        );

        return ProcessingResult.proceedWithAdditional(message, List.of(archiveCopy));
    }
}
