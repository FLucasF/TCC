package com.pousada.service;

import com.pousada.model.Message;
import com.pousada.model.MessageChannel;

import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private long nextId = 1;

    public void sendEmail(String to, String subject, String body) {
        Message message = new Message(nextId++, MessageChannel.EMAIL, to, subject, body);
        messages.add(message);
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
