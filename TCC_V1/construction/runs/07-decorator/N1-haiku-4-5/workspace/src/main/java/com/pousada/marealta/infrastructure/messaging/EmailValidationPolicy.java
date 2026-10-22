package com.pousada.marealta.infrastructure.messaging;

import com.pousada.marealta.util.EmailValidator;

public class EmailValidationPolicy implements MessagePolicy {
    @Override
    public MessagePolicyResult apply(MessageContext context) {
        if (!EmailValidator.isValid(context.originalRecipient)) {
            return MessagePolicyResult.blocked(context.originalRecipient);
        }
        return MessagePolicyResult.pass(context.originalRecipient, context.originalSubject, context.originalBody);
    }
}
