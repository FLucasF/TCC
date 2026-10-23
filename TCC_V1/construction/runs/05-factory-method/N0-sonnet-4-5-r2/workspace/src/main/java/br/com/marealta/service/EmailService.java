package br.com.marealta.service;

import br.com.marealta.model.Message;
import br.com.marealta.model.Reservation;
import br.com.marealta.repository.MessageRepository;

public class EmailService {
    private final MessageRepository messageRepository;

    public EmailService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation, String documentText) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmailBody(reservation, documentText);

        Message message = new Message("EMAIL", reservation.getGuestEmail(), subject, body);
        messageRepository.save(message);
    }

    private String buildConfirmationEmailBody(Reservation reservation, String documentText) {
        return String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            documentText
        );
    }
}
