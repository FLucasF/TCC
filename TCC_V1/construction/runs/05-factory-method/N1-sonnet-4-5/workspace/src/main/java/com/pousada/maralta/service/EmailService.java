package com.pousada.maralta.service;

import com.pousada.maralta.domain.Message;
import com.pousada.maralta.domain.Reservation;
import com.pousada.maralta.domain.ReservationDocument;

public class EmailService {
    public Message createConfirmationEmail(Reservation reservation, ReservationDocument document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";

        String body = "Olá, " + reservation.getGuestName() + "!\n\n" +
                     "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                     document.text() + "\n\n" +
                     "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                     "Até breve!";

        return new Message(null, "EMAIL", reservation.getGuestEmail(), subject, body);
    }
}
