package com.pousadamarealta.email;

import com.pousadamarealta.util.EmailValidator;

public class ValidationRule extends EmailRule {
    @Override
    protected boolean shouldProcess(EmailMessage message) {
        return true;
    }

    @Override
    protected void doProcess(EmailMessage message) {
        if (!EmailValidator.isValid(message.getTo())) {
            message.block();
        }
    }
}
