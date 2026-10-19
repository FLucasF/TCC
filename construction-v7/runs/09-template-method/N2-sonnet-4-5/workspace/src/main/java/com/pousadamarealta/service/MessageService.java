package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.repository.MessageRepository;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendEmail(String to, String subject, String body) {
        long id = messageRepository.getNextId();
        Message message = new Message(id, MessageChannel.EMAIL, to, subject, body);
        messageRepository.save(message);
    }
}
