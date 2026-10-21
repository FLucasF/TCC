package com.pousadamarealta.messaging;

import java.util.List;

public class TestModeRule implements MessageRule {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    @Override
    public List<OutboundMessage> apply(OutboundMessage message) {
        if (message.isBlocked()) {
            return List.of(message);
        }

        String originalTo = message.getTo();
        message.setTo(TEST_EMAIL);
        message.setSubject("[TESTE] " + message.getSubject());
        message.setBody("Destinatário original: " + originalTo + "\n" + message.getBody());

        return List.of(message);
    }
}
