package pousada.domain;

import pousada.identity.ReservationDocument;

/** O e-mail de confirmação: sempre em português, com o documento da reserva no meio. */
public final class ConfirmationEmail {
    public static final String SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private ConfirmationEmail() {
    }

    public static Message of(Reservation r) {
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + ReservationDocument.text(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(0, Channel.EMAIL, r.guestEmail(), SUBJECT, body);
    }
}
