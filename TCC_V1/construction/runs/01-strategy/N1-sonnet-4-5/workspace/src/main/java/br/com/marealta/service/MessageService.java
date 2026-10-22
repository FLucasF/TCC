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

        Message message = new Message(null, "EMAIL", reservation.getGuestEmail(), subject, body);
        messageRepository.save(message);
    }

    private String buildConfirmationEmail(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(DocumentService.buildReceipt(reservation));
        sb.append("\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
