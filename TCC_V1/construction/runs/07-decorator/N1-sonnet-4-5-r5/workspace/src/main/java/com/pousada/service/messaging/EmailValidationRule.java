package com.pousada.service.messaging;

public class EmailValidationRule implements MessageRule {

    @Override
    public ProcessingResult process(OutgoingMessage message) {
        if (!isValidEmail(message.getTo())) {
            message.block();
        }
        return new ProcessingResult();
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
