package com.pousada.marealta.messaging.processors;

import com.pousada.marealta.messaging.MessageData;
import com.pousada.marealta.messaging.MessageProcessor;
import com.pousada.marealta.domain.MessageStatus;

public class EmailValidationProcessor implements MessageProcessor {
    private final boolean enabled;

    public EmailValidationProcessor(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void process(MessageData message) {
        if (!enabled || message.isBlocked()) {
            return;
        }

        if (!isValidEmail(message.getTo())) {
            message.setStatus(MessageStatus.BLOCKED);
            message.setShouldGenerateCopy(false);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex < 1) {
            return false;
        }

        int lastAtIndex = email.lastIndexOf('@');
        if (atIndex != lastAtIndex) {
            return false;
        }

        String afterAt = email.substring(atIndex + 1);
        if (afterAt.isEmpty()) {
            return false;
        }

        int dotIndex = afterAt.indexOf('.');
        return dotIndex > 0 && dotIndex < afterAt.length() - 1;
    }
}
