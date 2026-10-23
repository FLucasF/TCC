package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;

import java.util.ArrayList;
import java.util.List;

public class MessagePipeline {
    private final List<MessageRule> rules;

    public MessagePipeline(List<MessageRule> rules) {
        this.rules = new ArrayList<>(rules);
    }

    public ProcessingResult process(MessageRequest request) {
        MessageContext context = new MessageContext(request);

        for (MessageRule rule : rules) {
            context = rule.apply(context);
            if (context.isBlocked()) {
                break;
            }
        }

        return buildResult(context);
    }

    private ProcessingResult buildResult(MessageContext context) {
        List<Message> messages = new ArrayList<>();

        MessageRequest current = context.getCurrent();
        MessageStatus status = context.isBlocked() ? MessageStatus.BLOCKED : MessageStatus.SENT;
        messages.add(new Message(null, current.getChannel(), current.getTo(),
                                 current.getSubject(), current.getBody(), status));

        if (!context.isBlocked()) {
            for (MessageRequest additional : context.getAdditional()) {
                messages.add(new Message(null, additional.getChannel(), additional.getTo(),
                                       additional.getSubject(), additional.getBody(), MessageStatus.SENT));
            }
        }

        return new ProcessingResult(messages);
    }

    public static class ProcessingResult {
        private final List<Message> messages;

        public ProcessingResult(List<Message> messages) {
            this.messages = messages;
        }

        public List<Message> getMessages() {
            return messages;
        }
    }
}
