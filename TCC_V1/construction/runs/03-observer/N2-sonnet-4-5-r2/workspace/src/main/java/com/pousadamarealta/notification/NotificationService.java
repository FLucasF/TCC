package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.repository.MessageRepository;

public class NotificationService {
    private final MessageRepository messageRepository;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendEmail(String to, String subject, String body) {
        Message message = new Message(MessageChannel.EMAIL, to, subject, body);
        messageRepository.save(message);
    }
}
