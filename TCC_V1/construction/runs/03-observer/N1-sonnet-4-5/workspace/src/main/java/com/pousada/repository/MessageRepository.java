package com.pousada.repository;

import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageRepository {
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Message save(MessageChannel channel, String to, String subject, String body) {
        Message message = new Message(idGenerator.getAndIncrement(), channel, to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> findAll() {
        return new ArrayList<>(messages);
    }
}
