package com.pousada.service;

import com.pousada.dao.MessageDAO;
import com.pousada.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private static final MessageDAO messageDAO = new MessageDAO();

    public static void sendEmail(String to, String subject, String body) {
        logger.info("Sending email to {} with subject: {}", to, subject);
        messageDAO.create("EMAIL", to, subject, body);
    }

    public List<Message> getAllMessages() {
        return messageDAO.findAll();
    }
}
