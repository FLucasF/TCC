package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.messaging.ProcessingContext;
import com.pousadamarealta.messaging.ProcessingResult;

public class ArchiveCopyRule implements MessageProcessor {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String ARCHIVE_PREFIX = "[Cópia] ";

    @Override
    public ProcessingResult process(Message message, ProcessingContext context) {
        if (context.isTestMode()) {
            return ProcessingResult.continueProcessing(message);
        }

        Message archiveCopy = new Message();
        archiveCopy.setTo(ARCHIVE_EMAIL);
        archiveCopy.setSubject(ARCHIVE_PREFIX + message.getSubject());
        archiveCopy.setBody(message.getBody());

        return ProcessingResult.continueWithAdditional(message, archiveCopy);
    }
}
