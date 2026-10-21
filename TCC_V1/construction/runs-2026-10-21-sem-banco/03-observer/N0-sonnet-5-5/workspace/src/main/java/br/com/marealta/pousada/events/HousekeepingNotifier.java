package br.com.marealta.pousada.events;

import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;

public class HousekeepingNotifier implements ReservationListener {
    public static final String ADDRESS = "governanca@pousadamarealta.com.br";
    private final Outbox outbox;

    public HousekeepingNotifier(Outbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void on(ReservationEvent event) {
        switch (event) {
            case ReservationEvent.Confirmed(Reservation r) -> outbox.sendEmail(ADDRESS,
                    "Reserva confirmada - quarto " + r.room().number(),
                    "Reserva nº " + r.id() + " confirmada para o quarto " + r.room().number() + ".\n"
                            + "Entrada: " + Receipt.DATE.format(r.checkIn())
                            + " | Saída: " + Receipt.DATE.format(r.checkOut()) + "\n"
                            + "Hóspedes: " + r.guests());
            case ReservationEvent.CancelledAfterConfirmation(Reservation r) -> outbox.sendEmail(ADDRESS,
                    "Reserva cancelada - quarto " + r.room().number(),
                    "Reserva nº " + r.id() + " cancelada. O quarto " + r.room().number()
                            + " está livre nas datas:\n"
                            + "Entrada: " + Receipt.DATE.format(r.checkIn())
                            + " | Saída: " + Receipt.DATE.format(r.checkOut()));
        }
    }
}
