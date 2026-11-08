package com.pousadamarealta.service;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.MessageChannel;
import com.pousadamarealta.repository.MessageRepository;

import java.sql.SQLException;
import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            Message message = new Message(MessageChannel.EMAIL, to, subject, body);
            messageRepository.save(message);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> getAllMessages() {
        try {
            return messageRepository.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve messages", e);
        }
    }
}
