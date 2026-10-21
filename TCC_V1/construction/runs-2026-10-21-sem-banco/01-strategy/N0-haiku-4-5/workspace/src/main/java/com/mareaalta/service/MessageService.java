package com.mareaalta.service;

import com.mareaalta.model.Message;
import java.util.*;

public class MessageService {
    private List<Message> messages = new ArrayList<>();
    private int nextId = 1;

    public Message sendEmail(String to, String subject, String body) {
        Message message = new Message(nextId++, "EMAIL", to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
