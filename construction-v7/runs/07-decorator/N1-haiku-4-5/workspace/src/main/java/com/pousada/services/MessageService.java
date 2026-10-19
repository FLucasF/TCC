package com.pousada.services;

import com.pousada.models.Message;
import com.pousada.models.MessageStatus;
import com.pousada.utils.EmailValidator;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private long nextMessageId = 1;
    private final boolean testMode;
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    public MessageService(boolean testMode) {
        this.testMode = testMode;
    }

    public Message sendEmail(String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            Message message = new Message(nextMessageId++, "EMAIL", to, subject, body, MessageStatus.BLOCKED);
            messages.add(message);
            return message;
        }

        String actualTo = to;
        String actualSubject = subject;
        String actualBody = body;

        if (testMode) {
            actualTo = TEST_EMAIL;
            actualSubject = "[TESTE] " + subject;
            actualBody = "Destinatário original: " + to + "\n" + body;
        }

        Message message = new Message(nextMessageId++, "EMAIL", actualTo, actualSubject, actualBody, MessageStatus.SENT);
        messages.add(message);

        if (!testMode && EmailValidator.isValid(to)) {
            Message archiveCopy = new Message(
                    nextMessageId++,
                    "EMAIL",
                    ARCHIVE_EMAIL,
                    "[Cópia] " + subject,
                    body,
                    MessageStatus.SENT);
            messages.add(archiveCopy);
        }

        return message;
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public void clearMessages() {
        messages.clear();
        nextMessageId = 1;
    }
}
