package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Formats;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationListener;
import br.com.marealta.pousada.domain.ReservationStatus;

/** Avisa a governança para preparar o quarto, ou que as datas ficaram livres. */
public class HousekeepingNotice implements ReservationListener {

    private final MessageLog log;
    private final String address;

    public HousekeepingNotice(MessageLog log, String address) {
        this.log = log;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation r) {
        log.send(Channel.EMAIL, address, "Reserva confirmada - quarto " + r.room().number(),
                "A reserva " + r.id() + " do quarto " + r.room().number() + " foi confirmada.\n"
                        + "Entrada: " + Formats.date(r.checkIn()) + "\n"
                        + "Saída: " + Formats.date(r.checkOut()) + "\n"
                        + "Hóspedes: " + r.guests());
    }

    @Override
    public void onCancelled(Reservation r, ReservationStatus previous) {
        if (previous != ReservationStatus.CONFIRMED) {
            return;
        }
        log.send(Channel.EMAIL, address, "Reserva cancelada - quarto " + r.room().number(),
                "A reserva " + r.id() + " do quarto " + r.room().number() + " foi cancelada.\n"
                        + "Datas liberadas: de " + Formats.date(r.checkIn()) + " a " + Formats.date(r.checkOut()));
    }
}
