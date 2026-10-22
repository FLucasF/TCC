package com.pousadamareaalta.service;

import com.pousadamareaalta.model.Message;
import com.pousadamareaalta.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendEmail(String to, String subject, String body) {
        Message message = new Message("EMAIL", to, subject, body);
        messageRepository.save(message);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
