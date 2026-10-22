package com.pousada.marealta.messaging;

import com.pousada.marealta.domain.Message;
import java.util.ArrayList;
import java.util.List;

public class MessageProcessor {
    private final List<MessageProcessingRule> rules;

    public MessageProcessor() {
        this.rules = new ArrayList<>();
    }

    public void addRule(MessageProcessingRule rule) {
        rules.add(rule);
    }

    public List<Message> process(Message originalMessage) {
        List<Message> result = new ArrayList<>();
        result.add(originalMessage);

        MessageProcessingContext context = new MessageProcessingContext(result);

        for (MessageProcessingRule rule : rules) {
            rule.process(originalMessage, context);
        }

        return context.getAllMessages();
    }
}
