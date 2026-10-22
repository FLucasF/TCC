package com.pousada.marealta.service;

import com.pousada.marealta.db.MessageRepository;
import com.pousada.marealta.model.Message;
import com.pousada.marealta.model.MessageStatus;
import com.pousada.marealta.util.EmailValidator;

import java.util.List;

public class MessageService {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final boolean IS_TEST_MODE = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

    public static Message sendMessage(String to, String subject, String body) throws Exception {
        MessageStatus status = MessageStatus.SENT;

        if (!EmailValidator.isValid(to)) {
            status = MessageStatus.BLOCKED;
            return MessageRepository.save("EMAIL", to, subject, body, status);
        }

        String finalTo = to;
        String finalSubject = subject;
        String finalBody = body;

        if (IS_TEST_MODE) {
            finalTo = TEST_EMAIL;
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + to + "\n\n" + body;
        }

        Message message = MessageRepository.save("EMAIL", finalTo, finalSubject, finalBody, status);

        if (status == MessageStatus.SENT && !IS_TEST_MODE) {
            String copySubject = "[Cópia] " + subject;
            MessageRepository.save("EMAIL", ARCHIVE_EMAIL, copySubject, body, MessageStatus.SENT);
        }

        return message;
    }

    public static List<Message> getAllMessages() throws Exception {
        return MessageRepository.findAll();
    }
}
