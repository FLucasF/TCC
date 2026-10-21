package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.util.EmailValidator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {
    private final List<Message> messages;
    private final AtomicLong messageIdCounter;
    private final boolean testMode;
    private final String archiveEmail;

    public MessageService() {
        this.messages = new ArrayList<>();
        this.messageIdCounter = new AtomicLong(0);
        this.testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        this.archiveEmail = "arquivo@pousadamarealta.com.br";
    }

    public void sendEmail(String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            addMessage(MessageChannel.EMAIL, to, subject, body, MessageStatus.BLOCKED);
            return;
        }

        if (testMode) {
            String originalTo = to;
            String testTo = "testes@pousadamarealta.com.br";
            String testSubject = "[TESTE] " + subject;
            String testBody = "Destinatário original: " + originalTo + "\n" + body;

            addMessage(MessageChannel.EMAIL, testTo, testSubject, testBody, MessageStatus.SENT);
        } else {
            addMessage(MessageChannel.EMAIL, to, subject, body, MessageStatus.SENT);

            String archiveSubject = "[Cópia] " + subject;
            addMessage(MessageChannel.EMAIL, archiveEmail, archiveSubject, body, MessageStatus.SENT);
        }
    }

    private void addMessage(MessageChannel channel, String to, String subject, String body, MessageStatus status) {
        long messageId = messageIdCounter.incrementAndGet();
        Message message = new Message(messageId, channel, to, subject, body, status);
        messages.add(message);
    }

    public List<Message> getAllMessages() {
        return Collections.unmodifiableList(messages);
    }
}
