package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.messaging.ProcessingResult;

public final class EmailValidationRule implements MessageRule {
    @Override
    public ProcessingResult process(OutgoingMessage message) {
        if (!isValidEmail(message.to())) {
            return ProcessingResult.block(message);
        }
        return ProcessingResult.proceed(message);
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

        int dotIndex = domain.indexOf('.');
        if (dotIndex == 0 || dotIndex == domain.length() - 1) {
            return false;
        }

        return true;
    }
}
