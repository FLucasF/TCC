package com.pousada.domain.service;

import com.pousada.domain.model.Message;
import com.pousada.domain.model.MessageChannel;
import com.pousada.infrastructure.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message sendMessage(MessageChannel channel, String to, String subject, String body) {
        Long id = messageRepository.nextId();
        Message message = new Message(id, channel, to, subject, body);
        return messageRepository.save(message);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
