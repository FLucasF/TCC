package com.pousadamarealta.service;

import java.sql.SQLException;
import java.util.List;
import com.pousadamarealta.dao.MessageDAO;
import com.pousadamarealta.models.Message;
import com.pousadamarealta.models.MessageChannel;
import com.pousadamarealta.models.MessageStatus;
import com.pousadamarealta.util.EmailValidator;

public class MessageService {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final boolean TEST_MODE = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

    public static Message sendEmail(String to, String subject, String body) throws SQLException {
        MessageStatus status;
        String finalRecipient = to;
        String finalSubject = subject;
        String finalBody = body;

        if (!EmailValidator.isValid(to)) {
            status = MessageStatus.BLOCKED;
        } else {
            status = MessageStatus.SENT;
            if (TEST_MODE) {
                finalRecipient = TEST_EMAIL;
                finalSubject = "[TESTE] " + subject;
                finalBody = "Destinatário original: " + to + "\n\n" + body;
            }
        }

        Message message = MessageDAO.create(MessageChannel.EMAIL, finalRecipient, finalSubject, finalBody, status);

        if (status == MessageStatus.SENT && !TEST_MODE && EmailValidator.isValid(to)) {
            sendArchiveCopy(subject, body);
        }

        return message;
    }

    private static void sendArchiveCopy(String subject, String body) throws SQLException {
        String finalSubject = "[Cópia] " + subject;
        MessageDAO.create(MessageChannel.EMAIL, ARCHIVE_EMAIL, finalSubject, body, MessageStatus.SENT);
    }

    public static List<Message> getAllMessages() throws SQLException {
        return MessageDAO.getAllMessages();
    }
}
