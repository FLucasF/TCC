package com.pousada.service;

import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private long nextId = 1;

    public Message sendEmail(String to, String subject, String body) {
        Message message = new Message(nextId++, MessageChannel.EMAIL, to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
