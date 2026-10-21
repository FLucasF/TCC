package br.com.pousada.service;

import br.com.pousada.model.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {

    private final List<Message> messages;
    private final AtomicLong messageIdCounter;

    public MessageService() {
        this.messages = new ArrayList<>();
        this.messageIdCounter = new AtomicLong(1);
    }

    public Message sendEmail(String to, String subject, String body) {
        Long id = messageIdCounter.getAndIncrement();
        Message message = new Message(id, "EMAIL", to, subject, body);
        messages.add(message);
        return message;
    }

    public List<Message> getAllMessages() {
        return List.copyOf(messages);
    }
}
