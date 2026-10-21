package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Message;
import java.util.ArrayList;
import java.util.List;

public class MessageStore {
    private final List<Message> messages = new ArrayList<>();

    public void addMessage(Message message) {
        messages.add(message);
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
