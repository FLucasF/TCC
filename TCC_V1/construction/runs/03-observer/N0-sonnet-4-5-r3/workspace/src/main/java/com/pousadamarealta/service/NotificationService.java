package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.repository.MessageRepository;

public class NotificationService {
    private final MessageRepository messageRepository;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendEmail(String to, String subject, String body) {
        Message message = new Message("EMAIL", to, subject, body);
        messageRepository.save(message);
    }
}
