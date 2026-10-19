package br.com.pousada.marealta.service;

import br.com.pousada.marealta.model.Message;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageRepository {
    private final Map<Long, Message> messages = new ConcurrentHashMap<>();
    private long nextMessageId = 1;

    public void save(Message message) {
        Message messageWithId = new Message(nextMessageId++, message.getChannel(), message.getTo(), message.getSubject(), message.getBody());
        messages.put(messageWithId.getId(), messageWithId);
    }

    public List<Message> getAll() {
        return messages.values().stream()
                .sorted(Comparator.comparingLong(Message::getId))
                .toList();
    }

    public Message getById(long id) {
        return messages.get(id);
    }
}
