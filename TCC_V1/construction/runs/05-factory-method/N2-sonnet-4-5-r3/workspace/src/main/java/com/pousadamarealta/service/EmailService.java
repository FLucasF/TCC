package com.pousadamarealta.service;

import com.pousadamarealta.document.DocumentGeneratorFactory;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.MessageRepository;

import java.sql.SQLException;

public class EmailService {
    private final MessageRepository messageRepository;
    private final DocumentGeneratorFactory documentGeneratorFactory;

    public EmailService(MessageRepository messageRepository, DocumentGeneratorFactory documentGeneratorFactory) {
        this.messageRepository = messageRepository;
        this.documentGeneratorFactory = documentGeneratorFactory;
    }

    public void sendConfirmationEmail(Reservation reservation, Room room) throws SQLException {
        Document document = documentGeneratorFactory.getGenerator(reservation).generate(reservation, room);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), document.getText());

        Message message = new Message(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageRepository.save(message);
    }
}
