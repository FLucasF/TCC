package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutgoingMessage;

public final class TestModeRule implements MessageRule {
    private static final String TEST_RECIPIENT = "testes@pousadamarealta.com.br";
    private static final String TEST_PREFIX = "[TESTE] ";

    @Override
    public boolean apply(OutgoingMessage message) {
        if (message.isBlocked()) {
            return true;
        }

        String originalRecipient = message.getTo();
        message.setTo(TEST_RECIPIENT);
        message.setSubject(TEST_PREFIX + message.getSubject());
        message.setBody("Destinatário original: " + originalRecipient + "\n" + message.getBody());

        return true;
    }
}
