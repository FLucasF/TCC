package com.pousada.marealta.messaging;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.util.EmailValidator;

public class EmailValidationRule implements MessageProcessingRule {
    @Override
    public void process(Message message, MessageProcessingContext context) {
        if (!EmailValidator.isValid(message.getTo())) {
            message.setStatus(Message.Status.BLOCKED);
        }
    }
}
