package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;

public class TestModeRule implements MessageRule {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final String TEST_PREFIX = "[TESTE] ";

    private final boolean testModeEnabled;

    public TestModeRule(boolean testModeEnabled) {
        this.testModeEnabled = testModeEnabled;
    }

    @Override
    public void apply(MessageContext context) {
        if (!testModeEnabled) {
            return;
        }

        Message message = context.getMessage();

        if (message.getStatus() == MessageStatus.BLOCKED) {
            return;
        }

        String originalRecipient = context.getOriginalRecipient();
        message.setTo(TEST_EMAIL);
        message.setSubject(TEST_PREFIX + message.getSubject());
        message.setBody("Destinatário original: " + originalRecipient + "\n" + message.getBody());
    }
}
