package com.pousadamarealta.messaging;

import java.util.List;

public class MessageProcessor {
    private final List<MessageRule> rules;

    public MessageProcessor(List<MessageRule> rules) {
        this.rules = rules;
    }

    public MessageRuleContext process(MessageRuleContext context) {
        for (MessageRule rule : rules) {
            rule.apply(context);
        }
        return context;
    }
}
