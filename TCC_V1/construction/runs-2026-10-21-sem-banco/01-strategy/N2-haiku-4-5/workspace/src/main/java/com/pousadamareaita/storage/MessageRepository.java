package com.pousadamareaita.storage;

import com.pousadamareaita.model.Message;
import java.util.*;

public class MessageRepository {
    private static final List<Message> messages = new ArrayList<>();
    private static int nextId = 1;

    public Message save(Message message) {
        messages.add(message);
        return message;
    }

    public List<Message> findAll() {
        return new ArrayList<>(messages);
    }

    public int getNextId() {
        return nextId++;
    }
}
