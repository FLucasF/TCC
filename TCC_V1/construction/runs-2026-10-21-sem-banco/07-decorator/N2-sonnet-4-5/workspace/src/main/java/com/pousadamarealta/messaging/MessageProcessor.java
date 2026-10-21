package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;

import java.util.List;

public class MessageProcessor {
    private final List<MessageRule> rules;

    public MessageProcessor(List<MessageRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<Message> process(List<Message> messages) {
        List<Message> current = messages;
        for (MessageRule rule : rules) {
            current = rule.process(current);
        }
        return current;
    }
}
