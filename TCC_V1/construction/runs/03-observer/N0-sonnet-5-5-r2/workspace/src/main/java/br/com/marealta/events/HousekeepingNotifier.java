package br.com.marealta.events;

import br.com.marealta.domain.OutgoingMessage;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;

/** Avisa a governança para preparar (ou liberar) o quarto. */
public final class HousekeepingNotifier implements ReservationListener {
    public static final String ADDRESS = "governanca@pousadamarealta.com.br";

    private final MessageSender sender;

    public HousekeepingNotifier(MessageSender sender) {
        this.sender = sender;
    }

    @Override
    public void onConfirmed(ReservationEvent.Confirmed e) {
        Reservation r = e.reservation();
        String body = "A reserva nº " + r.id() + " foi confirmada para o quarto " + e.room().number()
                + " (" + e.room().type() + ").\n"
                + "Entrada: " + Receipt.formatDate(r.checkIn()) + "\n"
                + "Saída: " + Receipt.formatDate(r.checkOut()) + "\n"
                + "Hóspedes: " + r.guests();
        sender.send(OutgoingMessage.email(ADDRESS, "Reserva confirmada - quarto " + e.room().number(), body));
    }

    @Override
    public void onCancelled(ReservationEvent.Cancelled e) {
        if (!e.wasConfirmed()) {
            return;
        }
        Reservation r = e.reservation();
        String body = "A reserva nº " + r.id() + " do quarto " + e.room().number()
                + " (" + e.room().type() + ") foi cancelada. Estas datas ficaram livres:\n"
                + "Entrada: " + Receipt.formatDate(r.checkIn()) + "\n"
                + "Saída: " + Receipt.formatDate(r.checkOut());
        sender.send(OutgoingMessage.email(ADDRESS, "Reserva cancelada - quarto " + e.room().number(), body));
    }
}
