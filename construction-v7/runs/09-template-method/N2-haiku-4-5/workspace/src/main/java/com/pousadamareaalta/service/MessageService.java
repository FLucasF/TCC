package com.pousadamareaalta.service;

import com.pousadamareaalta.model.Message;
import java.util.*;

public class MessageService {
    private final List<Message> messages;
    private long nextId = 1;

    public MessageService() {
        this.messages = new ArrayList<>();
    }

    public Message sendEmail(String to, String subject, String body) {
        Message message = new Message(nextId++, "EMAIL", to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }
}
