package br.com.marealta;

public final class ConfirmationEmail {
    public static final String SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private ConfirmationEmail() {
    }

    public static String body(Reservation r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(r).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
