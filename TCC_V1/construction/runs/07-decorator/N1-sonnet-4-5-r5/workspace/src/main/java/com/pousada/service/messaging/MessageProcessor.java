package com.pousada.service.messaging;

import java.util.ArrayList;
import java.util.List;

public class MessageProcessor {
    private final List<MessageRule> rules;

    public MessageProcessor(List<MessageRule> rules) {
        this.rules = rules;
    }

    public List<OutgoingMessage> process(OutgoingMessage message) {
        List<OutgoingMessage> allMessages = new ArrayList<>();
        allMessages.add(message);

        for (MessageRule rule : rules) {
            List<OutgoingMessage> newMessages = new ArrayList<>();

            for (OutgoingMessage msg : allMessages) {
                if (!msg.isBlocked()) {
                    ProcessingResult result = rule.process(msg);
                    newMessages.addAll(result.getAdditionalMessages());
                }
            }

            allMessages.addAll(newMessages);
        }

        return allMessages;
    }
}
