package br.com.marealta.reservas.notification;

import br.com.marealta.reservas.domain.Receipt;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.ReservationListener;
import br.com.marealta.reservas.domain.ReservationStatus;

import java.time.LocalDate;

public class HousekeepingNotifier implements ReservationListener {
    public static final String ADDRESS = "governanca@pousadamarealta.com.br";
    private final MessageOutbox outbox;

    public HousekeepingNotifier(MessageOutbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void onConfirmed(Reservation r) {
        outbox.email(ADDRESS, "Reserva confirmada - quarto " + r.room().number(),
                "O quarto " + r.room().number() + " foi reservado.\n"
                        + "Entrada: " + date(r.checkIn()) + "\n"
                        + "Saída: " + date(r.checkOut()) + "\n"
                        + "Hóspedes: " + r.guests());
    }

    @Override
    public void onCancelled(Reservation r, ReservationStatus previousStatus) {
        if (previousStatus != ReservationStatus.CONFIRMED) return;
        outbox.email(ADDRESS, "Reserva cancelada - quarto " + r.room().number(),
                "A reserva do quarto " + r.room().number() + " foi cancelada.\n"
                        + "Datas liberadas: de " + date(r.checkIn()) + " a " + date(r.checkOut()));
    }

    private static String date(LocalDate date) {
        return Receipt.date(date);
    }
}
