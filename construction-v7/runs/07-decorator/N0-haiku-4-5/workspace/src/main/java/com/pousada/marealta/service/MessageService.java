package com.pousada.marealta.service;

import com.pousada.marealta.model.Message;
import com.pousada.marealta.util.EmailValidator;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final boolean testMode;

    public MessageService(boolean testMode) {
        this.testMode = testMode;
    }

    public Message sendMessage(String channel, String to, String subject, String body) {
        String status = "SENT";
        String finalTo = to;
        String finalSubject = subject;
        String finalBody = body;

        if (!EmailValidator.isValid(to)) {
            status = "BLOCKED";
            addMessage(channel, to, subject, body, status);
            return new Message(nextId.get() - 1, channel, to, subject, body, status);
        }

        if (testMode) {
            finalTo = "testes@pousadamarealta.com.br";
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + to + "\n\n" + body;
        }

        long id = addMessage(channel, finalTo, finalSubject, finalBody, status);

        if (!testMode) {
            String archiveSubject = "[Cópia] " + subject;
            addMessage(channel, "arquivo@pousadamarealta.com.br", archiveSubject, body, status);
        }

        return new Message(id, channel, finalTo, finalSubject, finalBody, status);
    }

    private long addMessage(String channel, String to, String subject, String body, String status) {
        long id = nextId.getAndIncrement();
        messages.add(new Message(id, channel, to, subject, body, status));
        return id;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
