package com.pousadamarealta.service;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {
    private final List<Message> messages;
    private final AtomicLong nextId;

    public MessageService() {
        this.messages = new ArrayList<>();
        this.nextId = new AtomicLong(1);
    }

    public Message sendEmail(String to, String subject, String body) {
        Message message = new Message(
                nextId.getAndIncrement(),
                MessageChannel.EMAIL,
                to,
                subject,
                body
        );
        messages.add(message);
        return message;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
