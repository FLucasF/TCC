package com.pousada.service;

import com.pousada.domain.Message;
import java.util.*;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private long nextMessageId = 1;

    public void sendEmail(String to, String subject, String body) {
        Message message = new Message(nextMessageId++, "EMAIL", to, subject, body);
        messages.add(message);
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
