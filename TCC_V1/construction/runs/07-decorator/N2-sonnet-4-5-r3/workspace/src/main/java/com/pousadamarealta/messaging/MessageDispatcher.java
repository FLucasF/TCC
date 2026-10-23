package com.pousadamarealta.messaging;

import java.util.List;

public final class MessageDispatcher {
    private final List<MessageRule> rules;

    public MessageDispatcher(List<MessageRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public MessageContext dispatch(OutgoingMessage message) {
        MessageContext context = new MessageContext(message);
        for (MessageRule rule : rules) {
            rule.process(context);
        }
        return context;
    }
}
