package com.pousada.marealta.messaging;

import com.pousada.marealta.config.AppConfig;
import com.pousada.marealta.domain.Message;

public class TestModeRedirectRule implements MessageProcessingRule {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final String TEST_PREFIX = "[TESTE] ";
    private static final String ORIGINAL_RECIPIENT_PREFIX = "Destinatário original: ";

    @Override
    public void process(Message message, MessageProcessingContext context) {
        if (!AppConfig.isTestMode() || message.getStatus() == Message.Status.BLOCKED) {
            return;
        }

        String originalTo = message.getTo();
        message.setTo(TEST_EMAIL);
        message.setSubject(TEST_PREFIX + message.getSubject());
        message.setBody(ORIGINAL_RECIPIENT_PREFIX + originalTo + "\n\n" + message.getBody());
    }
}
