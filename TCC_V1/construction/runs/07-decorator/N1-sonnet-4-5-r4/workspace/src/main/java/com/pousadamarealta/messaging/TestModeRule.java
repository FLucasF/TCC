package com.pousadamarealta.messaging;

import com.pousadamarealta.model.Message;

public class TestModeRule implements MessageRule {

    @Override
    public void apply(MessageRuleContext context) {
        if (context.isBlocked()) {
            return;
        }

        Message current = context.getCurrentMessage();
        String originalRecipient = current.getTo();

        current.setTo("testes@pousadamarealta.com.br");
        current.setSubject("[TESTE] " + current.getSubject());
        current.setBody("Destinatário original: " + originalRecipient + "\n" + current.getBody());
    }
}
