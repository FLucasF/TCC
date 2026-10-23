package com.pousadamarealta.messaging;

public final class EmailValidationRule implements MessageRule {
    @Override
    public void process(MessageContext context) {
        String email = context.getMessage().to();
        if (!isValidEmail(email)) {
            context.setStatus(MessageStatus.BLOCKED);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);

        if (localPart.isBlank() || domainPart.isBlank()) {
            return false;
        }

        return domainPart.contains(".");
    }
}
