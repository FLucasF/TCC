package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public class MessageProcessor {
    private final List<MessageRule> rules;

    public MessageProcessor(List<MessageRule> rules) {
        this.rules = rules;
    }

    public List<ProcessedMessage> process(PendingMessage pending) {
        ProcessedMessage message = new ProcessedMessage(pending);

        for (MessageRule rule : rules) {
            rule.apply(message);
        }

        List<ProcessedMessage> result = new ArrayList<>();
        result.add(message);
        result.addAll(message.getAdditionalMessages());
        return result;
    }
}
