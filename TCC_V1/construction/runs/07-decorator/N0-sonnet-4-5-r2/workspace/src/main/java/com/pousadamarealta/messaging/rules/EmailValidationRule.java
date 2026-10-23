package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.MessageStatus;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.messaging.ProcessingContext;
import com.pousadamarealta.messaging.ProcessingResult;

public class EmailValidationRule implements MessageProcessor {

    @Override
    public ProcessingResult process(Message message, ProcessingContext context) {
        if (!isValidEmail(message.getTo())) {
            message.setStatus(MessageStatus.BLOCKED);
            return ProcessingResult.block(message);
        }
        return ProcessingResult.continueProcessing(message);
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        if (localPart.isEmpty() || domain.isEmpty()) {
            return false;
        }

        return domain.contains(".");
    }
}
