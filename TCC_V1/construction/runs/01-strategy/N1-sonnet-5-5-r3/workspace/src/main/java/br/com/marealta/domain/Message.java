package br.com.marealta.domain;

public record Message(long id, String channel, String to, String subject, String body) {
    public static final String EMAIL = "EMAIL";

    public static Message reservationConfirmed(Reservation reservation, Receipt receipt) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt.render() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(0, EMAIL, reservation.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
