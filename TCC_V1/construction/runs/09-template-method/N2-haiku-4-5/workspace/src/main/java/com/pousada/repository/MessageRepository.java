package com.pousada.repository;

import com.pousada.domain.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageRepository {
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Message save(String channel, String to, String subject, String body) {
        long id = nextId.getAndIncrement();
        Message message = new Message(id, channel, to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> findAll() {
        return new ArrayList<>(messages);
    }
}
