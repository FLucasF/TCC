package br.com.marealta.notification;

import br.com.marealta.app.Notification;
import br.com.marealta.domain.Formats;
import br.com.marealta.domain.Reservation;

import java.util.List;

/** A governança prepara o quarto quando a reserva é confirmada e sabe quando as datas ficam livres. */
public class HousekeepingSubscriber implements Subscriber {
    static final String ADDRESS = "governanca@pousadamarealta.com.br";

    @Override
    public List<Notification> onConfirmed(ReservationEvent.Confirmed event) {
        Reservation r = event.reservation();
        String body = "A reserva nº " + r.id() + " foi confirmada para o quarto " + r.roomNumber() + ".\n"
                + "Entrada: " + Formats.date(r.checkIn()) + "\n"
                + "Saída: " + Formats.date(r.checkOut()) + "\n"
                + "Hóspedes: " + r.guests();
        return List.of(Notification.email(ADDRESS, "Reserva confirmada - quarto " + r.roomNumber(), body));
    }

    @Override
    public List<Notification> onCancelled(ReservationEvent.Cancelled event) {
        Reservation r = event.reservation();
        String body = "A reserva nº " + r.id() + " do quarto " + r.roomNumber() + " foi cancelada.\n"
                + "Datas que ficaram livres: de " + Formats.date(r.checkIn())
                + " a " + Formats.date(r.checkOut()) + " (saída).";
        return List.of(Notification.email(ADDRESS, "Reserva cancelada - quarto " + r.roomNumber(), body));
    }
}
