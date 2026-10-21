package com.pousadamarealta.repository;

import com.pousadamarealta.model.Message;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MessageRepository {
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Message save(String channel, String to, String subject, String body) {
        Message message = new Message(idGenerator.getAndIncrement(), channel, to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> findAll() {
        return new ArrayList<>(messages);
    }
}
