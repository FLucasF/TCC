package com.pousadamarealta.service;

import com.pousadamarealta.db.MessageRepository;
import com.pousadamarealta.model.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MessageService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final boolean IS_TEST_MODE = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public List<Message> sendMessage(String originalTo, String subject, String body) throws Exception {
        List<Message> sentMessages = new ArrayList<>();

        if (!isValidEmail(originalTo)) {
            Message blockedMessage = new Message(null, "EMAIL", originalTo, subject, body, "BLOCKED");
            messageRepository.save(blockedMessage);
            sentMessages.add(blockedMessage);
            return sentMessages;
        }

        String recipientEmail = originalTo;
        String finalSubject = subject;
        String finalBody = body;

        if (IS_TEST_MODE) {
            recipientEmail = TEST_EMAIL;
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + originalTo + "\n\n" + body;
        }

        Message mainMessage = new Message(null, "EMAIL", recipientEmail, finalSubject, finalBody, "SENT");
        messageRepository.save(mainMessage);
        sentMessages.add(mainMessage);

        if (!IS_TEST_MODE) {
            Message archiveMessage = new Message(
                null,
                "EMAIL",
                ARCHIVE_EMAIL,
                "[Cópia] " + subject,
                body,
                "SENT"
            );
            messageRepository.save(archiveMessage);
            sentMessages.add(archiveMessage);
        }

        return sentMessages;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
