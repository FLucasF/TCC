package br.com.maretalta.service;

import br.com.maretalta.domain.Receipt;
import br.com.maretalta.domain.Reservation;

/** Textos das mensagens enviadas ao hóspede. */
final class Mailer {
    static final String CHANNEL = "EMAIL";
    static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private Mailer() {}

    static String confirmationBody(Reservation r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(r).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
