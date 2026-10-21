package com.pousada.marealta.service;

import java.util.ArrayList;
import java.util.List;
import com.pousada.marealta.model.Message;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private long nextMessageId = 1;

    public Message sendMessage(String channel, String to, String subject, String body) {
        Message message = new Message(nextMessageId++, channel, to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
