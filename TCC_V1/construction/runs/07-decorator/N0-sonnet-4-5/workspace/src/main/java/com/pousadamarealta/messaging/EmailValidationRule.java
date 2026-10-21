package com.pousadamarealta.messaging;

import java.util.List;

public class EmailValidationRule implements MessageRule {
    @Override
    public List<OutboundMessage> apply(OutboundMessage message) {
        if (!isValidEmail(message.getTo())) {
            message.setBlocked(true);
        }
        return List.of(message);
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
        if (atIndex == 0 || atIndex == email.length() - 1) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        if (!domain.contains(".")) {
            return false;
        }

        return true;
    }
}
