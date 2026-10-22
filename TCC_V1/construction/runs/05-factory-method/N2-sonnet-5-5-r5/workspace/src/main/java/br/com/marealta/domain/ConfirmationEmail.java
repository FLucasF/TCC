package br.com.marealta.domain;

/** E-mail de confirmação: sempre em português; muda só o documento que vai dentro. */
public final class ConfirmationEmail {
    private ConfirmationEmail() {
    }

    public static Message compose(Reservation reservation) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + reservation.document().text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(0, Message.Channel.EMAIL, reservation.guestEmail(),
                "Reserva confirmada - Pousada Maré Alta", body);
    }
}
