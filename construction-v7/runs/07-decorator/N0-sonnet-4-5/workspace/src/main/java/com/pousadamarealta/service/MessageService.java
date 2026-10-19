package com.pousadamarealta.service;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;
import com.pousadamarealta.model.MessageStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong messageIdCounter = new AtomicLong(1);
    private final boolean testMode;

    public MessageService() {
        this.testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
    }

    public void sendEmail(String to, String subject, String body) {
        if (!isValidEmail(to)) {
            Message blockedMessage = new Message(
                messageIdCounter.getAndIncrement(),
                MessageChannel.EMAIL,
                to,
                subject,
                body,
                MessageStatus.BLOCKED
            );
            messages.add(blockedMessage);
            return;
        }

        String actualTo = to;
        String actualSubject = subject;
        String actualBody = body;

        if (testMode) {
            actualTo = TEST_EMAIL;
            actualSubject = "[TESTE] " + subject;
            actualBody = "Destinatário original: " + to + "\n" + body;
        }

        Message sentMessage = new Message(
            messageIdCounter.getAndIncrement(),
            MessageChannel.EMAIL,
            actualTo,
            actualSubject,
            actualBody,
            MessageStatus.SENT
        );
        messages.add(sentMessage);

        if (!testMode) {
            Message archiveCopy = new Message(
                messageIdCounter.getAndIncrement(),
                MessageChannel.EMAIL,
                ARCHIVE_EMAIL,
                "[Cópia] " + subject,
                body,
                MessageStatus.SENT
            );
            messages.add(archiveCopy);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        if (domain.isEmpty() || !domain.contains(".")) {
            return false;
        }

        int lastDotIndex = domain.lastIndexOf('.');
        if (lastDotIndex == 0 || lastDotIndex == domain.length() - 1) {
            return false;
        }

        return true;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
