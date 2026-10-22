package br.com.marealta;

public final class ConfirmationEmail {
    private ConfirmationEmail() {}

    public static Message of(Reservation r) {
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + r.identity().format().render(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(null, "EMAIL", r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
