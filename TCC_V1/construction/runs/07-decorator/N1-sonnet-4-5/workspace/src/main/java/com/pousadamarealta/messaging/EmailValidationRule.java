package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageStatus;

public class EmailValidationRule implements MessageRule {
    @Override
    public void apply(ProcessedMessage message) {
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

        String afterAt = email.substring(atIndex + 1);
        if (afterAt.isEmpty()) {
            return false;
        }

        return afterAt.contains(".");
    }
}
