package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.messaging.MessagePipeline;
import com.pousadamarealta.messaging.MessageRequest;
import com.pousadamarealta.repository.MessageRepository;

import java.util.ArrayList;
import java.util.List;

public class MessagingService {
    private final MessageRepository messageRepository;
    private final MessagePipeline pipeline;

    public MessagingService(MessageRepository messageRepository, MessagePipeline pipeline) {
        this.messageRepository = messageRepository;
        this.pipeline = pipeline;
    }

    public List<Message> send(String to, String subject, String body) {
        MessageRequest request = new MessageRequest(MessageChannel.EMAIL, to, subject, body);
        MessagePipeline.ProcessingResult result = pipeline.process(request);

        List<Message> savedMessages = new ArrayList<>();
        for (Message message : result.getMessages()) {
            savedMessages.add(messageRepository.save(message));
        }

        return savedMessages;
    }

    public List<Message> findAll() {
        return messageRepository.findAll();
    }
}
