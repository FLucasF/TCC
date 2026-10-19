package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.messaging.ProcessedMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final MessageProcessor processor;

    public MessageService(boolean testMode) {
        this.processor = new MessageProcessor(testMode);
    }

    public void send(OutgoingMessage outgoing) {
        List<ProcessedMessage> processed = processor.process(outgoing);

        for (ProcessedMessage pm : processed) {
            Message message = new Message(
                nextId.getAndIncrement(),
                pm.getChannel(),
                pm.getTo(),
                pm.getSubject(),
                pm.getBody(),
                pm.getStatus()
            );
            messages.add(message);
        }
    }

    public List<Message> getAllMessages() {
        return List.copyOf(messages);
    }
}
