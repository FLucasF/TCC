package com.pousada.services;

import com.pousada.models.Message;
import com.pousada.utils.EmailValidator;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private long nextMessageId = 1;
    private final List<Message> messages = new ArrayList<>();
    private final boolean testMode;
    private final boolean archiveEnabled;

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    public MessageService(boolean testMode, boolean archiveEnabled) {
        this.testMode = testMode;
        this.archiveEnabled = archiveEnabled;
    }

    public void sendMessage(String recipientEmail, String subject, String body) {
        Message.Status status = Message.Status.SENT;
        String finalEmail = recipientEmail;
        String finalSubject = subject;
        String finalBody = body;

        if (!EmailValidator.isValid(recipientEmail)) {
            status = Message.Status.BLOCKED;
            addMessage(Message.Channel.EMAIL, recipientEmail, subject, body, status);
            return;
        }

        if (testMode) {
            finalEmail = TEST_EMAIL;
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + recipientEmail + "\n" + body;
        }

        addMessage(Message.Channel.EMAIL, finalEmail, finalSubject, finalBody, status);

        if (!testMode && archiveEnabled && status == Message.Status.SENT) {
            String archiveSubject = "[Cópia] " + subject;
            addMessage(Message.Channel.EMAIL, ARCHIVE_EMAIL, archiveSubject, body, Message.Status.SENT);
        }
    }

    private void addMessage(Message.Channel channel, String to, String subject, String body, Message.Status status) {
        Message message = new Message(nextMessageId++, channel, to, subject, body, status);
        messages.add(message);
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public long getMessageCount() {
        return messages.size();
    }

    public void clear() {
        messages.clear();
        nextMessageId = 1;
    }
}
