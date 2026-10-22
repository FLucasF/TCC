package br.pousada.marealta.service;

import br.pousada.marealta.model.Message;
import br.pousada.marealta.model.MessageChannel;
import br.pousada.marealta.model.MessageStatus;
import br.pousada.marealta.database.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final boolean TEST_MODE = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

    public static void sendConfirmationEmail(Connection conn, long reservationId, String guestName, String guestEmail, String documentText) throws Exception {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, guestName, documentText);

        sendMessage(conn, MessageChannel.EMAIL, guestEmail, subject, body);
    }

    public static void sendMessage(Connection conn, MessageChannel channel, String recipient, String subject, String body) throws Exception {
        List<Message> messages = processMessage(channel, recipient, subject, body);

        for (Message msg : messages) {
            MessageRepository.insert(conn, msg);
            logger.info("Message stored - Status: {}, To: {}", msg.getStatus(), msg.getTo());
        }
    }

    private static List<Message> processMessage(MessageChannel channel, String recipient, String subject, String body) {
        List<Message> messages = new ArrayList<>();

        if (!EmailValidator.isValid(recipient)) {
            messages.add(new Message(0, channel, recipient, subject, body, MessageStatus.BLOCKED));
            return messages;
        }

        String finalRecipient = recipient;
        String finalSubject = subject;
        String finalBody = body;

        if (TEST_MODE) {
            finalRecipient = TEST_EMAIL;
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + recipient + "\n\n" + body;
        }

        messages.add(new Message(0, channel, finalRecipient, finalSubject, finalBody, MessageStatus.SENT));

        if (!TEST_MODE) {
            messages.add(new Message(0, channel, ARCHIVE_EMAIL, "[Cópia] " + subject, body, MessageStatus.SENT));
        }

        return messages;
    }
}
