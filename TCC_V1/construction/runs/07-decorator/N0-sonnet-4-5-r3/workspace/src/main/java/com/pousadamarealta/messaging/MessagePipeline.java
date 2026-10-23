package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public class MessagePipeline {
    private final List<MessageRule> rules;

    public MessagePipeline(List<MessageRule> rules) {
        this.rules = rules;
    }

    public ProcessingResult process(OutgoingMessage message) {
        ProcessingResult result = ProcessingResult.allow(message);

        for (MessageRule rule : rules) {
            result = rule.process(message);

            if (result.isBlocked()) {
                return result;
            }
        }

        return result;
    }

    public static class Builder {
        private final List<MessageRule> rules = new ArrayList<>();

        public Builder addRule(MessageRule rule) {
            rules.add(rule);
            return this;
        }

        public MessagePipeline build() {
            return new MessagePipeline(rules);
        }
    }
}
