package com.pousadamarealta.messaging;

import java.util.List;

public class TestModeHandler implements MessageHandler {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private final boolean testMode;

    public TestModeHandler(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void handle(OutgoingMessage message, List<Message> sentMessages) {
        if (testMode && !message.isBlocked()) {
            String originalRecipient = message.getTo();
            message.setTo(TEST_EMAIL);
            message.setSubject("[TESTE] " + message.getSubject());
            message.setBody("Destinatário original: " + originalRecipient + "\n" + message.getBody());
        }
    }
}
