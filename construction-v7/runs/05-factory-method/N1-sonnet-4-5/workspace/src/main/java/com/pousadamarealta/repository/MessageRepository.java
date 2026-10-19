package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Message;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MessageRepository {
    private final Map<Long, Message> messages = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Message save(Message message) {
        messages.put(message.getId(), message);
        return message;
    }

    public List<Message> findAll() {
        return new ArrayList<>(messages.values());
    }

    public long nextId() {
        return idGenerator.getAndIncrement();
    }
}
