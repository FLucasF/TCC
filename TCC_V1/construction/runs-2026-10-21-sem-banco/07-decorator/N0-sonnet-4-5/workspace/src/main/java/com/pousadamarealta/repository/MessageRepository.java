package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Message;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MessageRepository {
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Message save(Message message) {
        messages.add(message);
        return message;
    }

    public List<Message> findAll() {
        return new ArrayList<>(messages);
    }

    public Long nextId() {
        return idGenerator.getAndIncrement();
    }
}
