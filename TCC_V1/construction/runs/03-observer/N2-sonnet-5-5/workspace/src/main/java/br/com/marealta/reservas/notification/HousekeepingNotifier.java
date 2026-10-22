package br.com.marealta.reservas.notification;

import br.com.marealta.reservas.domain.Money;
import br.com.marealta.reservas.domain.Reservation;

/** Avisa a governança para preparar o quarto, ou que as noites ficaram livres. */
public class HousekeepingNotifier implements ReservationListener {
    private final MessageLog log;
    private final String address;

    public HousekeepingNotifier(MessageLog log, String address) {
        this.log = log;
        this.address = address;
    }

    @Override
    public void onConfirmed(ReservationConfirmed event) {
        Reservation r = event.reservation();
        String body = "A reserva nº " + r.id() + " do quarto " + event.room().number() + " foi confirmada.\n"
                + "Entrada: " + Money.brDate(r.checkIn()) + "\n"
                + "Saída: " + Money.brDate(r.checkOut()) + "\n"
                + "Hóspedes: " + r.guests();
        log.sendEmail(address, "Reserva confirmada - quarto " + event.room().number(), body);
    }

    @Override
    public void onCancelled(ReservationCancelled event) {
        Reservation r = event.reservation();
        if (!r.wasPaid()) {
            return;
        }
        String body = "A reserva nº " + r.id() + " do quarto " + event.room().number() + " foi cancelada.\n"
                + "O quarto fica livre de " + Money.brDate(r.checkIn()) + " (entrada) a "
                + Money.brDate(r.checkOut()) + " (saída).";
        log.sendEmail(address, "Reserva cancelada - quarto " + event.room().number(), body);
    }
}
