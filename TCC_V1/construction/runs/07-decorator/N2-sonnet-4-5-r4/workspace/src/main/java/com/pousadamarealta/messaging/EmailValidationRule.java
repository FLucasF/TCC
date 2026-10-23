package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;

public class EmailValidationRule implements MessageRule {

    @Override
    public void apply(MessageContext context) {
        Message message = context.getMessage();

        if (!isValidEmail(message.getTo())) {
            message.setStatus(MessageStatus.BLOCKED);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        long atCount = email.chars().filter(ch -> ch == '@').count();
        if (atCount != 1) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex == 0) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        if (domain.isEmpty()) {
            return false;
        }

        return domain.contains(".");
    }
}
