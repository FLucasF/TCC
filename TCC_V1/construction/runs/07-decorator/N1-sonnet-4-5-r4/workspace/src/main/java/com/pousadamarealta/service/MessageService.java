package com.pousadamarealta.service;

import com.pousadamarealta.messaging.MessageRulePipeline;
import com.pousadamarealta.messaging.MessageRulePipelineFactory;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageRulePipeline pipeline;

    public MessageService(MessageRepository messageRepository, boolean testMode) {
        this.messageRepository = messageRepository;
        this.pipeline = MessageRulePipelineFactory.create(testMode);
    }

    public void send(Message message) {
        List<Message> messages = pipeline.process(message);

        for (Message msg : messages) {
            messageRepository.save(msg);
        }
    }

    public List<Message> findAll() {
        return messageRepository.findAll();
    }
}
