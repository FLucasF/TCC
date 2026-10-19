package com.pousadamarealta.messaging;

import java.util.List;
import java.util.regex.Pattern;

public class EmailValidationHandler implements MessageHandler {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@.]+.*$");

    @Override
    public void handle(OutgoingMessage message, List<Message> sentMessages) {
        if (!isValidEmail(message.getTo())) {
            message.setBlocked(true);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String trimmed = email.trim();
        long atCount = trimmed.chars().filter(ch -> ch == '@').count();
        if (atCount != 1) {
            return false;
        }

        int atIndex = trimmed.indexOf('@');
        if (atIndex == 0) {
            return false;
        }

        String afterAt = trimmed.substring(atIndex + 1);
        if (afterAt.isEmpty() || !afterAt.contains(".")) {
            return false;
        }

        return EMAIL_PATTERN.matcher(trimmed).matches();
    }
}
