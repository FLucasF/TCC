package com.pousada.services;

import com.pousada.models.Message;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong messageIdCounter = new AtomicLong(1);

    public Message sendMessage(String channel, String to, String subject, String body) {
        long id = messageIdCounter.getAndIncrement();
        Message message = new Message(id, channel, to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
