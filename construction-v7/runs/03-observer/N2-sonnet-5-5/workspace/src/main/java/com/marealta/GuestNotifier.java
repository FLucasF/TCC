package com.marealta;

/** E-mail de confirmação para o hóspede, com o comprovante. */
public class GuestNotifier implements ReservationListener {
    private final Outbox outbox;

    public GuestNotifier(Outbox outbox) { this.outbox = outbox; }

    @Override
    public void onConfirmed(Reservation r) {
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(r).toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        outbox.send(Channel.EMAIL, r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
