package com.pousadamarealta.application.notification;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.repository.MessageRepository;

public class NotificationService {
    private final MessageRepository messageRepository;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendEmail(String to, String subject, String body) {
        Message message = Message.email(to, subject, body);
        messageRepository.save(message);
    }
}
