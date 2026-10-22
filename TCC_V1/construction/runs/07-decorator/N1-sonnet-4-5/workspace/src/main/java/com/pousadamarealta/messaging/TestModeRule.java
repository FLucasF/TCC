package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageStatus;

public class TestModeRule implements MessageRule {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    @Override
    public void apply(ProcessedMessage message) {
        if (message.getStatus() == MessageStatus.BLOCKED) {
            return;
        }

        String originalTo = message.getTo();
        message.setTo(TEST_EMAIL);
        message.setSubject("[TESTE] " + message.getSubject());
        message.setBody("Destinatário original: " + originalTo + "\n" + message.getBody());
    }
}
