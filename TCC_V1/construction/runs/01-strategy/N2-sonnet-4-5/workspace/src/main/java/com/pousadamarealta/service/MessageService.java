package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.util.DocumentFormatter;
import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String document = DocumentFormatter.formatDocument(reservation);

        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            document
        );

        Message message = new Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageRepository.save(message);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
