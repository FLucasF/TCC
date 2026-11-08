package br.com.marealta.service;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.Reservation;
import br.com.marealta.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(reservation);

        Message message = new Message("EMAIL", reservation.getGuestEmail(), subject, body);
        messageRepository.save(message);
    }

    public List<Message> findAllMessages() {
        return messageRepository.findAll();
    }

    private String buildConfirmationEmail(Reservation reservation) {
        DocumentService documentService = new DocumentService();
        String receipt = documentService.generateReceiptText(reservation);

        return String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""",
            reservation.getGuestName(),
            receipt
        );
    }
}
