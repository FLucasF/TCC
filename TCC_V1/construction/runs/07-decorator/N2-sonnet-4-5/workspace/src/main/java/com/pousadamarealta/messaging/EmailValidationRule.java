package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;

import java.util.List;
import java.util.regex.Pattern;

public class EmailValidationRule implements MessageRule {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");

    @Override
    public List<Message> process(List<Message> messages) {
        return messages.stream()
            .map(this::validate)
            .toList();
    }

    private Message validate(Message message) {
        if (!isValidEmail(message.to())) {
            return message.withStatus(MessageStatus.BLOCKED);
        }
        return message;
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
