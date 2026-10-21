package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {
    private final AtomicLong nextId = new AtomicLong(1);
    private final List<Message> messages = new ArrayList<>();

    public void send(MessageChannel channel, String to, String subject, String body) {
        Message message = new Message(nextId.getAndIncrement(), channel, to, subject, body);
        messages.add(message);
    }

    public List<Message> getAll() {
        return new ArrayList<>(messages);
    }
}
