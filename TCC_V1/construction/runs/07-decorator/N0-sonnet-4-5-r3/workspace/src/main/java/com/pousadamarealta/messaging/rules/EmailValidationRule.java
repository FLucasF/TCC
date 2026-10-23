package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.messaging.ProcessingResult;

public class EmailValidationRule implements MessageRule {

    @Override
    public ProcessingResult process(OutgoingMessage message) {
        if (!isValidEmail(message.getTo())) {
            return ProcessingResult.block(message);
        }
        return ProcessingResult.allow(message);
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

        String domain = email.substring(atPosition + 1);

        return domain.contains(".");
    }
}
