package br.com.mare.pousada;

public class Notifier {
    private final MessageRepository messages;

    public Notifier(MessageRepository messages) {
        this.messages = messages;
    }

    public void reservationConfirmed(Reservation reservation) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + reservation.identity().document(reservation).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        messages.add(Channel.EMAIL, reservation.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
