package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutboundMessage;
import com.pousadamarealta.messaging.RuleContext;

public class EmailValidationRule implements MessageRule {

    @Override
    public void apply(OutboundMessage message, RuleContext context) {
        if (!isValidEmail(message.getTo())) {
            message.setBlocked(true);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
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
