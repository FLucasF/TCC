package com.pousadamarealta.service;

import com.pousadamarealta.database.MessageDAO;
import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;

public class MessageService {
    private static final MessageService INSTANCE = new MessageService();

    private MessageService() {
    }

    public static MessageService getInstance() {
        return INSTANCE;
    }

    public void sendMessage(MessageChannel channel, String to, String subject, String body) throws Exception {
        Message message = new Message(channel, to, subject, body);
        MessageDAO.getInstance().insertMessage(message);
    }
}
