package com.pousada.repository;

import com.pousada.model.Message;

import java.util.*;

public class MessageRepository {
    private final List<Message> messages = new ArrayList<>();
    private long nextId = 1;

    public Message save(String channel, String to, String subject, String body) {
        long id = nextId++;
        Message message = new Message(id, channel, to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> findAll() {
        return new ArrayList<>(messages);
    }
}
