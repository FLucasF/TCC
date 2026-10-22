package com.pousada.marealta.services;

import com.pousada.marealta.db.Database;
import com.pousada.marealta.models.Message;
import com.pousada.marealta.models.MessageStatus;
import com.pousada.marealta.util.EmailValidator;

import java.sql.SQLException;

public class EmailService {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final String ARCHIVE_PREFIX = "[Cópia] ";
    private static final String TEST_PREFIX = "[TESTE] ";

    private final Database database;
    private final boolean testMode;

    public EmailService(Database database, boolean testMode) {
        this.database = database;
        this.testMode = testMode;
    }

    public void sendConfirmationEmail(String guestName, String guestEmail, String receiptText) throws SQLException {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationBody(guestName, receiptText);

        sendEmail(guestEmail, subject, body);
    }

    public void sendEmail(String toEmail, String subject, String body) throws SQLException {
        boolean isValidEmail = EmailValidator.isValid(toEmail);

        if (!isValidEmail) {
            database.createMessage("EMAIL", toEmail, subject, body, MessageStatus.BLOCKED);
            return;
        }

        String finalEmail = toEmail;
        String finalSubject = subject;
        String finalBody = body;

        if (testMode) {
            finalEmail = TEST_EMAIL;
            finalSubject = TEST_PREFIX + subject;
            finalBody = "Destinatário original: " + toEmail + "\n" + body;
        }

        database.createMessage("EMAIL", finalEmail, finalSubject, finalBody, MessageStatus.SENT);

        if (!testMode) {
            String archiveBody = body;
            String archiveSubject = ARCHIVE_PREFIX + subject;
            database.createMessage("EMAIL", ARCHIVE_EMAIL, archiveSubject, archiveBody, MessageStatus.SENT);
        }
    }

    private String buildConfirmationBody(String guestName, String receiptText) {
        return "Olá, " + guestName + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               receiptText + "\n\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!";
    }
}
