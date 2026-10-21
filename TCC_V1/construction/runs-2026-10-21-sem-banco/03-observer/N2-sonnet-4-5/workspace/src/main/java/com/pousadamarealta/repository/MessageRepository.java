package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.notification.MessageSender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class MessageRepository implements MessageSender {
    private final List<Message> messages = new ArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    @Override
    public void send(MessageChannel channel, String to, String subject, String body) {
        Message message = new Message(nextId.getAndIncrement(), channel, to, subject, body);
        messages.add(message);
    }

    public List<Message> findAll() {
        return List.copyOf(messages);
    }
}
