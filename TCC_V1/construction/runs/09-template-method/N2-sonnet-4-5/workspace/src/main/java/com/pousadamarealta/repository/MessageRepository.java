package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Message;

import java.util.*;

public class MessageRepository {
    private final List<Message> messages = new ArrayList<>();
    private long nextId = 1;

    public Message save(Message message) {
        messages.add(message);
        return message;
    }

    public List<Message> findAll() {
        return new ArrayList<>(messages);
    }

    public long nextId() {
        return nextId++;
    }
}
