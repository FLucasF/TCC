package br.com.marealta.notification;

import br.com.marealta.domain.Reservation;

/** Avisa o hóspede, com o comprovante, quando a reserva é confirmada. */
public final class GuestNotifier implements ReservationListener {

    private final MessageSender sender;

    public GuestNotifier(MessageSender sender) {
        this.sender = sender;
    }

    @Override
    public void onConfirmed(Reservation reservation) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + reservation.receipt().text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        sender.send(Message.email(reservation.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body));
    }
}
