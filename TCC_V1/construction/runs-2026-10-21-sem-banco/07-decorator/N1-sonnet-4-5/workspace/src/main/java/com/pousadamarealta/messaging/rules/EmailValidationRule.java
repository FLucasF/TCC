package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.MessageRuleContext;

public class EmailValidationRule implements MessageRule {

    @Override
    public void apply(MessageRuleContext context) {
        if (!isValidEmail(context.getTo())) {
            context.setStatus(MessageStatus.BLOCKED);
        }
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
        if (atIndex == 0 || atIndex == email.length() - 1) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        return domain.contains(".");
    }
}
