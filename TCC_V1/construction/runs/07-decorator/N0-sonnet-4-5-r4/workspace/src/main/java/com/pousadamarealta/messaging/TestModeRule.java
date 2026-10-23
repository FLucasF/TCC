package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;

import java.util.Collections;
import java.util.List;

public class TestModeRule implements MessageRule {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    @Override
    public List<Message> apply(Message message) {
        if (message.getStatus() == MessageStatus.SENT) {
            String originalTo = message.getTo();
            message.setTo(TEST_EMAIL);
            message.setSubject("[TESTE] " + message.getSubject());
            message.setBody("Destinatário original: " + originalTo + "\n" + message.getBody());
        }

        return Collections.singletonList(message);
    }
}
