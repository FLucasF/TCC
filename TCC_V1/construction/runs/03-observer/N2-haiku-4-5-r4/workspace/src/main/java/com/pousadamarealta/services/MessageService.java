package com.pousadamarealta.services;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private final Database database;
    private long messageIdCounter = 1;

    public MessageService(Database database) {
        this.database = database;
    }

    public void sendMessage(MessageChannel channel, String to, String subject, String body) {
        Message message = new Message(messageIdCounter++, channel, to, subject, body);
        database.insertMessage(message);
    }

    public List<Message> getAllMessages() {
        return database.getAllMessages();
    }
}
