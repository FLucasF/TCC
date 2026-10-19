package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageStatus;

import java.util.Collections;
import java.util.List;

public class TestModeRule implements MessageRule {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    @Override
    public List<ProcessedMessage> apply(OutgoingMessage message, boolean isTestMode) {
        if (!isTestMode) {
            return Collections.emptyList();
        }

        String newSubject = "[TESTE] " + message.getSubject();
        String newBody = "Destinatário original: " + message.getTo() + "\n" + message.getBody();

        return List.of(new ProcessedMessage(
            message.getChannel(),
            TEST_EMAIL,
            newSubject,
            newBody,
            MessageStatus.SENT
        ));
    }
}
