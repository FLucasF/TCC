package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.MessageRuleContext;

public class TestModeRedirectRule implements MessageRule {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    @Override
    public void apply(MessageRuleContext context) {
        if (context.getStatus() == MessageStatus.SENT) {
            context.setTo(TEST_EMAIL);
            context.setSubject("[TESTE] " + context.getSubject());
            context.setBody("Destinatário original: " + context.getOriginalTo() + "\n" + context.getBody());
        }
    }
}
