package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public final class MessageProcessor {
    private final List<MessageRule> rules;

    public MessageProcessor(List<MessageRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public ProcessedMessages process(OutgoingMessage message) {
        OutgoingMessage current = message;
        List<OutgoingMessage> additionalMessages = new ArrayList<>();

        for (MessageRule rule : rules) {
            ProcessingResult result = rule.process(current);

            if (result.isBlocked()) {
                return ProcessedMessages.blocked(message);
            }

            current = result.processedMessage();
            additionalMessages.addAll(result.additionalMessages());
        }

        return ProcessedMessages.sent(current, additionalMessages);
    }

    public record ProcessedMessages(
        boolean blocked,
        OutgoingMessage originalMessage,
        OutgoingMessage finalMessage,
        List<OutgoingMessage> additionalMessages
    ) {
        static ProcessedMessages blocked(OutgoingMessage original) {
            return new ProcessedMessages(true, original, original, List.of());
        }

        static ProcessedMessages sent(OutgoingMessage finalMessage, List<OutgoingMessage> additional) {
            return new ProcessedMessages(false, null, finalMessage, List.copyOf(additional));
        }
    }
}
