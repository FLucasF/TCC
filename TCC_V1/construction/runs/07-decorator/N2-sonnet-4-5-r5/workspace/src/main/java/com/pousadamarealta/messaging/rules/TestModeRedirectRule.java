package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageContext;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutgoingMessage;

public class TestModeRedirectRule implements MessageRule {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final String SUBJECT_PREFIX = "[TESTE] ";
    private static final String BODY_PREFIX_TEMPLATE = "Destinatário original: %s\n";

    @Override
    public void process(MessageContext context) {
        if (!context.isBlocked()) {
            OutgoingMessage original = context.getMessage();
            String originalRecipient = original.getTo();

            OutgoingMessage redirected = original.withModifications(
                TEST_EMAIL,
                SUBJECT_PREFIX + original.getSubject(),
                String.format(BODY_PREFIX_TEMPLATE, originalRecipient) + original.getBody()
            );

            context.setMessage(redirected);
        }
    }
}
