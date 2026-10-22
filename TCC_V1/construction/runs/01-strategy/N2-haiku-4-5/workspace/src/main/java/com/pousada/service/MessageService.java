package com.pousada.service;

import com.pousada.domain.Message;
import com.pousada.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageService {
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);
    private final MessageRepository messageRepo;

    public MessageService(MessageRepository messageRepo) {
        this.messageRepo = messageRepo;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            messageRepo.save(Message.Channel.EMAIL, to, subject, body);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email", e);
            throw new RuntimeException(e);
        }
    }

    public List<Message> getAllMessages() {
        try {
            return messageRepo.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve messages", e);
            throw new RuntimeException(e);
        }
    }
}
