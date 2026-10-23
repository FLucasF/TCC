package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageProcessor {
    private final List<MessageRule> rules;

    public MessageProcessor(List<MessageRule> rules) {
        this.rules = new ArrayList<>(rules);
    }

    public List<Message> process(Message message) {
        MessageContext context = new MessageContext(message);

        for (MessageRule rule : rules) {
            rule.apply(context);
        }

        return context.getAllMessages();
    }
}
