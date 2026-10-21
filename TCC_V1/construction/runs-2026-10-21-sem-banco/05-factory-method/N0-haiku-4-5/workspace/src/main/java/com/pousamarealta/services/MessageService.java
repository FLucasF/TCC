package com.pousamarealta.services;

import com.pousamarealta.models.Message;
import java.util.*;

public class MessageService {
    private List<Message> messages = new ArrayList<>();
    private long nextMessageId = 1;

    public void sendMessage(String channel, String to, String subject, String body) {
        Message message = new Message(nextMessageId++, channel, to, subject, body);
        messages.add(message);
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
