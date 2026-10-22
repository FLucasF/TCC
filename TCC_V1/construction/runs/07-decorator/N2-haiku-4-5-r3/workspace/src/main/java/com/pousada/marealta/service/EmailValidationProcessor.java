package com.pousada.marealta.service;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageStatus;

public class EmailValidationProcessor implements MessageProcessor {
    private MessageProcessor nextProcessor;

    public void setNext(MessageProcessor next) {
        this.nextProcessor = next;
    }

    @Override
    public ProcessingResult process(Message message, MessageProcessingContext context) {
        if (!EmailValidator.isValid(message.getTo())) {
            message.setStatus(MessageStatus.BLOCKED);
            return new ProcessingResult(ProcessingResult.Status.BLOCKED, message);
        }

        if (nextProcessor != null) {
            return nextProcessor.process(message, context);
        }

        return new ProcessingResult(ProcessingResult.Status.COMPLETED, message);
    }
}
