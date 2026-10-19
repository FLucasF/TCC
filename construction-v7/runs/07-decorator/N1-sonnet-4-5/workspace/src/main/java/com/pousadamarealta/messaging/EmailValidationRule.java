package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageStatus;

import java.util.Collections;
import java.util.List;

public class EmailValidationRule implements MessageRule {
    @Override
    public List<ProcessedMessage> apply(OutgoingMessage message, boolean isTestMode) {
        if (!isValidEmail(message.getTo())) {
            return List.of(new ProcessedMessage(
                message.getChannel(),
                message.getTo(),
                message.getSubject(),
                message.getBody(),
                MessageStatus.BLOCKED
            ));
        }
        return Collections.emptyList();
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
        return domain.contains(".");
    }
}
