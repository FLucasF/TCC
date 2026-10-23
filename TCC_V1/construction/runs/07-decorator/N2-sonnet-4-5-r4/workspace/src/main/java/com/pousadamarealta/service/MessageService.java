package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
