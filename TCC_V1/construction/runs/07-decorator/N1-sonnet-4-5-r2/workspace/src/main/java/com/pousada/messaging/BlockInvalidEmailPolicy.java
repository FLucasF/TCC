package com.pousada.messaging;

import com.pousada.domain.Message;

public class BlockInvalidEmailPolicy implements MessagePolicy {

    @Override
    public void apply(PolicyResult result) {
        Message message = result.getMessage();
        if (!isValidEmail(message.getTo())) {
            result.transform(PolicyResult.block(message).getMessage());
        }
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
        if (atIndex == 0) {
            return false;
        }

        String afterAt = email.substring(atIndex + 1);
        if (afterAt.isEmpty() || !afterAt.contains(".")) {
            return false;
        }

        return true;
    }
}
