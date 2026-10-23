package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutgoingMessage;

public final class EmailValidationRule implements MessageRule {
    @Override
    public boolean apply(OutgoingMessage message) {
        if (!isValidEmail(message.getTo())) {
            message.block();
            return false;
        }
        return true;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        int atCount = 0;
        int atPosition = -1;

        for (int i = 0; i < email.length(); i++) {
            if (email.charAt(i) == '@') {
                atCount++;
                atPosition = i;
            }
        }

        if (atCount != 1) {
            return false;
        }

        if (atPosition == 0 || atPosition == email.length() - 1) {
            return false;
        }

        String afterAt = email.substring(atPosition + 1);
        int dotCount = 0;
        for (char c : afterAt.toCharArray()) {
            if (c == '.') {
                dotCount++;
            }
        }

        return dotCount >= 1;
    }
}
