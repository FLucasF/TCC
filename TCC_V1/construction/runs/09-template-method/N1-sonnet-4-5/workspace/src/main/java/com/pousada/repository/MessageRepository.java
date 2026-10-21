package com.pousada.repository;

import com.pousada.domain.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageRepository {
    private final List<Message> messages;
    private final AtomicLong nextId;

    public MessageRepository() {
        this.messages = new ArrayList<>();
        this.nextId = new AtomicLong(1);
    }

    public Message save(Message message) {
        messages.add(message);
        return message;
    }

    public List<Message> findAll() {
        return new ArrayList<>(messages);
    }

    public long getNextId() {
        return nextId.getAndIncrement();
    }
}
