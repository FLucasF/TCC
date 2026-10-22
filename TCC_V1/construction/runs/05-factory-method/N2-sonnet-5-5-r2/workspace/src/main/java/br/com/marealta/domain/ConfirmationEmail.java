package br.com.marealta.domain;

public final class ConfirmationEmail {
    public static final String SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private ConfirmationEmail() {
    }

    /** O e-mail é sempre em português; só o documento muda. */
    public static Message of(Reservation reservation) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + reservation.document().text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(0, "EMAIL", reservation.guestEmail(), SUBJECT, body);
    }
}
