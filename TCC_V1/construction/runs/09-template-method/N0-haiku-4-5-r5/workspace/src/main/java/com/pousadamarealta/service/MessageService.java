package com.pousadamarealta.service;

import com.pousadamarealta.model.Document;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.util.DocumentFormatter;

public class MessageService {
    private final MessageRepository messageRepository = new MessageRepository();

    public void sendConfirmationEmail(Reservation reservation, Document document) throws Exception {
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """,
            reservation.guestName,
            DocumentFormatter.formatAsText(document));

        messageRepository.create("EMAIL", reservation.guestEmail, "Reserva confirmada - Pousada Maré Alta", body);
    }
}
