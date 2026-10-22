package com.pousadamarealta.service;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.repository.MessageRepository;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final boolean TEST_MODE = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

    public static class SendResult {
        public Message original;
        public Message archiveCopy;

        public SendResult(Message original, Message archiveCopy) {
            this.original = original;
            this.archiveCopy = archiveCopy;
        }
    }

    public static SendResult sendMessage(String channel, String originalTo, String subject, String body) throws java.sql.SQLException {
        Message original = new Message();
        original.channel = channel;

        if (!isValidEmail(originalTo)) {
            original.to = originalTo;
            original.subject = subject;
            original.body = body;
            original.status = "BLOCKED";
            MessageRepository.save(original);
            return new SendResult(original, null);
        }

        String finalTo = originalTo;
        String finalSubject = subject;
        String finalBody = body;

        if (TEST_MODE) {
            finalTo = TEST_EMAIL;
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + originalTo + "\n\n" + body;
        }

        original.to = finalTo;
        original.subject = finalSubject;
        original.body = finalBody;
        original.status = "SENT";
        MessageRepository.save(original);

        Message archiveCopy = null;
        if (!TEST_MODE) {
            archiveCopy = new Message();
            archiveCopy.channel = channel;
            archiveCopy.to = ARCHIVE_EMAIL;
            archiveCopy.subject = "[Cópia] " + subject;
            archiveCopy.body = body;
            archiveCopy.status = "SENT";
            MessageRepository.save(archiveCopy);
        }

        return new SendResult(original, archiveCopy);
    }

    private static boolean isValidEmail(String email) {
        if (!email.contains("@")) return false;

        int atIndex = email.indexOf("@");
        if (atIndex != email.lastIndexOf("@")) return false;

        String before = email.substring(0, atIndex);
        if (before.isEmpty()) return false;

        String after = email.substring(atIndex + 1);
        if (!after.contains(".") || after.endsWith(".")) return false;

        return true;
    }
}
