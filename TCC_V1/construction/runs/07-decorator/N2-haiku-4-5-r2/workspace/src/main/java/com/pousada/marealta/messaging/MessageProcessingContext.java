package com.pousada.marealta.messaging;

import com.pousada.marealta.domain.Message;
import java.util.ArrayList;
import java.util.List;

public class MessageProcessingContext {
    private final List<Message> allMessages;

    public MessageProcessingContext(List<Message> allMessages) {
        this.allMessages = allMessages;
    }

    public List<Message> getAllMessages() {
        return allMessages;
    }

    public void addMessage(Message message) {
        allMessages.add(message);
    }
}
