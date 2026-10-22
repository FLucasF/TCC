package com.pousada.marealta.messaging.processors;

import com.pousada.marealta.messaging.MessageData;
import com.pousada.marealta.messaging.MessageProcessor;

public class TestModeProcessor implements MessageProcessor {
    private final boolean testModeEnabled;
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    public TestModeProcessor(boolean testModeEnabled) {
        this.testModeEnabled = testModeEnabled;
    }

    @Override
    public void process(MessageData message) {
        if (!testModeEnabled || message.isBlocked()) {
            return;
        }

        String originalTo = message.getTo();
        String originalBody = message.getBody();

        message.setTo(TEST_EMAIL);
        message.setSubject("[TESTE] " + message.getSubject());
        message.setBody("Destinatário original: " + originalTo + "\n\n" + originalBody);
        message.setShouldGenerateCopy(false);
    }
}
