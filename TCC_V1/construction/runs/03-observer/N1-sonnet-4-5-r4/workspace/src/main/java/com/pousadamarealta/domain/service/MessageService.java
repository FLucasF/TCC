package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.repository.MessageRepository;

import java.sql.SQLException;
import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void send(Message message) {
        try {
            messageRepository.save(message);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }

    public List<Message> getAllMessages() throws SQLException {
        return messageRepository.findAll();
    }
}
