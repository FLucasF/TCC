package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageRepository {
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Message save(Message message) {
        messages.add(message);
        return message;
    }

    public List<Message> findAll() {
        return List.copyOf(messages);
    }

    public long nextId() {
        return nextId.getAndIncrement();
    }
}
