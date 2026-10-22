package br.com.mealta.domain;

public final class ReservationMessages {
    private ReservationMessages() {
    }

    public static Message confirmation(Reservation reservation, Receipt receipt) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt.toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(null, Channel.EMAIL, reservation.guestEmail(),
                "Reserva confirmada - Pousada Maré Alta", body);
    }
}
