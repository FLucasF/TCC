package com.pousada.service;

import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {

    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public void sendEmail(String to, String subject, String body) {
        Message message = new Message(
            nextId.getAndIncrement(),
            MessageChannel.EMAIL,
            to,
            subject,
            body
        );
        messages.add(message);
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
