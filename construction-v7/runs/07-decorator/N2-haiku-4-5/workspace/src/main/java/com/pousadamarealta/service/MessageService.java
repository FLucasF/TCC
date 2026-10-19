package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;

import java.util.*;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private long nextId = 1;
    private final boolean testMode;

    public MessageService() {
        this.testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
    }

    public Message sendMessage(String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            Message blocked = new Message(nextId++, MessageChannel.EMAIL, to, subject, body, MessageStatus.BLOCKED);
            messages.add(blocked);
            return blocked;
        }

        if (testMode) {
            String originalRecipient = to;
            String testBody = "Destinatário original: " + originalRecipient + "\n\n" + body;
            Message testMessage = new Message(
                nextId++,
                MessageChannel.EMAIL,
                "testes@pousadamarealta.com.br",
                "[TESTE] " + subject,
                testBody,
                MessageStatus.SENT
            );
            messages.add(testMessage);
            return testMessage;
        } else {
            Message sent = new Message(nextId++, MessageChannel.EMAIL, to, subject, body, MessageStatus.SENT);
            messages.add(sent);

            Message archiveCopy = new Message(
                nextId++,
                MessageChannel.EMAIL,
                "arquivo@pousadamarealta.com.br",
                "[Cópia] " + subject,
                body,
                MessageStatus.SENT
            );
            messages.add(archiveCopy);

            return sent;
        }
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
