package br.com.pousada.service;

import br.com.pousada.domain.Document;
import br.com.pousada.domain.Message;
import br.com.pousada.repository.MessageRepository;

public class NotificationService {
    private final MessageRepository messageRepository;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(String guestName, String guestEmail, Document document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(guestName, document.text());

        Message message = new Message(null, "EMAIL", guestEmail, subject, body);
        messageRepository.save(message);
    }

    private String buildConfirmationEmail(String guestName, String documentText) {
        return String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", guestName, documentText);
    }
}
