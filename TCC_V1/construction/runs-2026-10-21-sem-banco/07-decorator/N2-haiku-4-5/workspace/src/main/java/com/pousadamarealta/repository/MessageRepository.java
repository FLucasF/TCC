package com.pousadamarealta.repository;

import com.pousadamarealta.model.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageRepository {
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Message save(Message message) {
        Message newMsg = new Message(
            nextId.getAndIncrement(),
            message.channel,
            message.to,
            message.subject,
            message.body,
            message.status
        );
        messages.add(newMsg);
        return newMsg;
    }

    public List<Message> findAll() {
        return new ArrayList<>(messages);
    }
}
