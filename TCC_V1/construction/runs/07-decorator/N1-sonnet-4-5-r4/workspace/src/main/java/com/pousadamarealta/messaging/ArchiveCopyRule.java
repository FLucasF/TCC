package com.pousadamarealta.messaging;

import com.pousadamarealta.model.Message;

public class ArchiveCopyRule implements MessageRule {

    @Override
    public void apply(MessageRuleContext context) {
        if (context.isBlocked()) {
            return;
        }

        Message current = context.getCurrentMessage();
        Message copy = new Message(
            current.getChannel(),
            "arquivo@pousadamarealta.com.br",
            "[Cópia] " + current.getSubject(),
            current.getBody()
        );

        context.addMessage(copy);
    }
}
