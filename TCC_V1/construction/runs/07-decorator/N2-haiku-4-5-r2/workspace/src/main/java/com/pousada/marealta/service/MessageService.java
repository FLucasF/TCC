package com.pousada.marealta.service;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.messaging.MessageProcessor;
import com.pousada.marealta.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageProcessor messageProcessor;

    public MessageService(MessageRepository messageRepository, MessageProcessor messageProcessor) {
        this.messageRepository = messageRepository;
        this.messageProcessor = messageProcessor;
    }

    public void sendMessage(Message message) {
        List<Message> processedMessages = messageProcessor.process(message);
        for (Message processedMessage : processedMessages) {
            messageRepository.save(processedMessage);
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
