package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageContext;
import com.pousadamarealta.messaging.MessageRule;

public class EmailValidationRule implements MessageRule {
    @Override
    public void process(MessageContext context) {
        String email = context.getMessage().getTo();
        if (!isValidEmail(email)) {
            context.getMessage().block();
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return false;
        }

        if (email.lastIndexOf('@') != atIndex) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        return !domain.isEmpty() && domain.contains(".");
    }
}
