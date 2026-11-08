package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.repository.MessageRepository;

import java.util.List;

public final class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void send(MessageChannel channel, String to, String subject, String body) {
        messageRepository.save(new Message(0, channel, to, subject, body));
    }

    public List<Message> listAll() {
        return messageRepository.findAll();
    }
}
