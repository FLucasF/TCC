package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public class MessagePipeline {
    private final List<MessageRule> rules;

    public MessagePipeline(List<MessageRule> rules) {
        this.rules = rules;
    }

    public List<OutboundMessage> process(OutboundMessage message) {
        List<OutboundMessage> messages = List.of(message);

        for (MessageRule rule : rules) {
            List<OutboundMessage> newMessages = new ArrayList<>();
            for (OutboundMessage msg : messages) {
                newMessages.addAll(rule.apply(msg));
            }
            messages = newMessages;
        }

        return messages;
    }
}
