package com.pousada.service;

import com.pousada.model.Message;
import com.pousada.model.Reservation;
import com.pousada.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation, String comprovante) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmailBody(reservation, comprovante);

        messageRepository.save("EMAIL", reservation.getGuestEmail(), subject, body);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    private String buildConfirmationEmailBody(Reservation reservation, String comprovante) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(comprovante).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }
}
