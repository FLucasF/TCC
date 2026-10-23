package com.pousada.domain.service;

import com.pousada.domain.model.Document;
import com.pousada.domain.model.Email;
import com.pousada.domain.model.Reservation;

public final class EmailService {

    public Email createConfirmationEmail(Reservation reservation, Document document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationBody(reservation.guestName(), document.text());

        return new Email(reservation.guestEmail(), subject, body);
    }

    private String buildConfirmationBody(String guestName, String documentText) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(documentText).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");

        return sb.toString();
    }
}
