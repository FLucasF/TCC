package com.pousada.service;

import com.pousada.domain.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final List<Message> messages = new ArrayList<>();
    private long nextId = 1;

    public Message save(Message message) {
        messages.add(message);
        return message;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }

    public long getNextId() {
        return nextId++;
    }
}
