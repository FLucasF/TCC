package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public class MessagePipeline {
    private final List<MessageRule> rules;

    public MessagePipeline(List<MessageRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<OutgoingMessage> process(OutgoingMessage message) {
        MessageContext context = new MessageContext(message);

        for (MessageRule rule : rules) {
            rule.process(context);
        }

        List<OutgoingMessage> result = new ArrayList<>();
        result.add(context.getMessage());
        result.addAll(context.getAdditionalMessages());

        return result;
    }
}
