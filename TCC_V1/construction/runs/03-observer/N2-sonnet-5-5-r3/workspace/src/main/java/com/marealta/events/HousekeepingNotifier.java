package com.marealta.events;

import com.marealta.domain.Channel;
import com.marealta.domain.MessageOutbox;
import com.marealta.domain.Reservation;
import com.marealta.domain.ReservationStatus;

/** A governança prepara o quarto quando a reserva é confirmada e libera quando uma reserva confirmada é cancelada. */
public final class HousekeepingNotifier implements ReservationListener {
    private final MessageOutbox outbox;
    private final String address;

    public HousekeepingNotifier(MessageOutbox outbox, String address) {
        this.outbox = outbox;
        this.address = address;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        Reservation r = event.reservation();
        int room = r.roomNumber();
        switch (event.type()) {
            case CONFIRMED -> outbox.send(Channel.EMAIL, address, "Reserva confirmada - quarto " + room,
                    "O quarto " + room + " foi reservado.\n"
                            + "Entrada: " + Dates.br(r.checkIn()) + "\n"
                            + "Saída: " + Dates.br(r.checkOut()) + "\n"
                            + "Hóspedes: " + r.guests());
            case CANCELLED -> {
                if (event.previousStatus() == ReservationStatus.CONFIRMED) {
                    outbox.send(Channel.EMAIL, address, "Reserva cancelada - quarto " + room,
                            "A reserva do quarto " + room + " foi cancelada.\n"
                                    + "Datas liberadas: de " + Dates.br(r.checkIn())
                                    + " (entrada) até " + Dates.br(r.checkOut()) + " (saída)");
                }
            }
            default -> { }
        }
    }
}
