package br.com.marealta.notification;

import br.com.marealta.domain.Reservation;

/** Avisa o hóspede, com o comprovante, que a reserva foi confirmada. */
public class GuestConfirmationEmail implements ReservationListener {
    private final MessageSender sender;

    public GuestConfirmationEmail(MessageSender sender) {
        this.sender = sender;
    }

    @Override
    public void on(ReservationEvent event) {
        if (event instanceof ReservationEvent.Confirmed confirmed) {
            Reservation reservation = confirmed.reservation();
            sender.email(reservation.guestEmail(), "Reserva confirmada - Pousada Maré Alta", """
                    Olá, %s!

                    Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                    %s

                    A entrada é a partir das 14h, e a saída, até as 12h.
                    Até breve!""".formatted(reservation.guestName(), reservation.receipt().toText()));
        }
    }
}
