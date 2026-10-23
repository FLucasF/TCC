package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;

import java.util.Collections;
import java.util.List;

public class EmailValidationRule implements MessageRule {
    @Override
    public List<Message> apply(Message message) {
        if (!isValidEmail(message.getTo())) {
            message.setStatus(MessageStatus.BLOCKED);
        } else {
            message.setStatus(MessageStatus.SENT);
        }
        return Collections.singletonList(message);
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }

        String beforeAt = email.substring(0, atIndex);
        String afterAt = email.substring(atIndex + 1);

        if (beforeAt.isEmpty() || afterAt.isEmpty()) {
            return false;
        }

        return afterAt.contains(".");
    }
}
