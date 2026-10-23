package com.pousadamarealta.messaging;

import java.util.List;

public final class MessageRuleChain {
    private final List<MessageRule> rules;

    public MessageRuleChain(List<MessageRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public void process(OutgoingMessage message) {
        for (MessageRule rule : rules) {
            if (!rule.apply(message)) {
                return;
            }
        }
    }
}
