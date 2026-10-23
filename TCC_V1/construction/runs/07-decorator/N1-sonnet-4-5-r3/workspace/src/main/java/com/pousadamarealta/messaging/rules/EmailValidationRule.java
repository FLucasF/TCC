package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageContext;
import com.pousadamarealta.messaging.MessageRule;

public class EmailValidationRule implements MessageRule {

    @Override
    public MessageContext apply(MessageContext context) {
        String email = context.getCurrent().getTo();

        if (!isValidEmail(email)) {
            return context.withBlocked(true);
        }

        return context;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        long atCount = email.chars().filter(ch -> ch == '@').count();
        if (atCount != 1) {
            return false;
        }

        int atIndex = email.indexOf('@');
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);

        if (localPart.isEmpty()) {
            return false;
        }

        if (!domainPart.contains(".")) {
            return false;
        }

        return true;
    }
}
