package com.pousada.marealta.service;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageStatus;

public class ArchiveProcessor implements MessageProcessor {
    private MessageProcessor nextProcessor;
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    public void setNext(MessageProcessor next) {
        this.nextProcessor = next;
    }

    @Override
    public ProcessingResult process(Message message, MessageProcessingContext context) {
        if (context.isCopyToArchiveEnabled() && !context.isTestMode()) {
            Message archiveCopy = new Message(
                message.getChannel(),
                ARCHIVE_EMAIL,
                "[Cópia] " + message.getSubject(),
                message.getBody(),
                MessageStatus.SENT
            );
            context.addGeneratedMessage(archiveCopy);
        }

        if (nextProcessor != null) {
            return nextProcessor.process(message, context);
        }

        return new ProcessingResult(ProcessingResult.Status.COMPLETED, message);
    }
}
