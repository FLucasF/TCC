package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutboundMessage;
import com.pousadamarealta.messaging.RuleContext;

public class TestModeRedirectRule implements MessageRule {
    private static final String TEST_RECIPIENT = "testes@pousadamarealta.com.br";

    @Override
    public void apply(OutboundMessage message, RuleContext context) {
        if (!context.isTestMode() || message.isBlocked()) {
            return;
        }

        context.setOriginalRecipient(message.getTo());
        message.setTo(TEST_RECIPIENT);
        message.setSubject("[TESTE] " + message.getSubject());
        message.setBody("Destinatário original: " + context.getOriginalRecipient() + "\n" + message.getBody());
    }
}
