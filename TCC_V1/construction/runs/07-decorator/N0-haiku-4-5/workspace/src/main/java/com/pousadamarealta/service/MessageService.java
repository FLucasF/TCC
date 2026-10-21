package com.pousadamarealta.service;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;
import com.pousadamarealta.model.MessageStatus;
import com.pousadamarealta.util.EmailValidator;

import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final List<Message> messages = new ArrayList<>();
    private long messageIdCounter = 1;
    private final boolean testMode;

    public MessageService(boolean testMode) {
        this.testMode = testMode;
    }

    public Message sendEmail(String to, String subject, String body) {
        Message message;

        if (!EmailValidator.isValid(to)) {
            message = new Message(messageIdCounter++, MessageChannel.EMAIL, to, subject, body, MessageStatus.BLOCKED);
            messages.add(message);
            return message;
        }

        String finalTo = to;
        String finalSubject = subject;
        String finalBody = body;

        if (testMode) {
            finalTo = TEST_EMAIL;
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + to + "\n\n" + body;
        }

        message = new Message(messageIdCounter++, MessageChannel.EMAIL, finalTo, finalSubject, finalBody, MessageStatus.SENT);
        messages.add(message);

        if (!testMode && EmailValidator.isValid(to)) {
            Message archiveMessage = new Message(messageIdCounter++, MessageChannel.EMAIL, ARCHIVE_EMAIL,
                    "[Cópia] " + subject, body, MessageStatus.SENT);
            messages.add(archiveMessage);
        }

        return message;
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public void clear() {
        messages.clear();
        messageIdCounter = 1;
    }
}
