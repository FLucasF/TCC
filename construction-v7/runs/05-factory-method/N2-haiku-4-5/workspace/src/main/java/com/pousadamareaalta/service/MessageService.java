package com.pousadamareaalta.service;

import com.pousadamareaalta.model.Message;

import java.util.*;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private long nextId = 1;

    public Message send(String channel, String to, String subject, String body) {
        Message message = new Message(nextId++, channel, to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> findAll() {
        return new ArrayList<>(messages);
    }
}
