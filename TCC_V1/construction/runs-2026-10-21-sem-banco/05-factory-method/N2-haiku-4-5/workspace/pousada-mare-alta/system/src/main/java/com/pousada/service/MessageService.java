package com.pousada.service;

import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private int nextMessageId = 1;

    public Message sendMessage(MessageChannel channel, String to, String subject, String body) {
        Message message = new Message(nextMessageId++, channel, to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
