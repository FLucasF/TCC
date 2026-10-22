package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.messaging.PendingMessage;
import com.pousadamarealta.messaging.ProcessedMessage;
import com.pousadamarealta.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageProcessor messageProcessor;

    public MessageService(MessageRepository messageRepository, MessageProcessor messageProcessor) {
        this.messageRepository = messageRepository;
        this.messageProcessor = messageProcessor;
    }

    public void send(PendingMessage pending) {
        List<ProcessedMessage> processed = messageProcessor.process(pending);

        for (ProcessedMessage pm : processed) {
            Message message = new Message(
                    pm.getChannel(),
                    pm.getTo(),
                    pm.getSubject(),
                    pm.getBody(),
                    pm.getStatus()
            );
            messageRepository.save(message);
        }
    }

    public List<Message> findAll() {
        return messageRepository.findAll();
    }
}
