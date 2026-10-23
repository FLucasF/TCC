package com.pousadamarealta.messaging;

import com.pousadamarealta.model.MessageStatus;

public class EmailValidationRule implements MessageRule {

    @Override
    public void apply(MessageRuleContext context) {
        String email = context.getCurrentMessage().getTo();

        if (!isValidEmail(email)) {
            context.block();
            context.getCurrentMessage().setStatus(MessageStatus.BLOCKED);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        long atCount = email.chars().filter(c -> c == '@').count();
        if (atCount != 1) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex == 0 || atIndex == email.length() - 1) {
            return false;
        }

        String beforeAt = email.substring(0, atIndex);
        String afterAt = email.substring(atIndex + 1);

        if (beforeAt.isEmpty()) {
            return false;
        }

        if (!afterAt.contains(".")) {
            return false;
        }

        return true;
    }
}
