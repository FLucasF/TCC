package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.DateFormats;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;

import java.util.List;

public class HousekeepingNotifier implements ReservationListener {

    static final String ADDRESS = "governanca@pousadamarealta.com.br";

    @Override
    public List<Message> onConfirmed(Reservation r, Room room) {
        String body = "A reserva " + r.id() + " do quarto " + room.number() + " foi confirmada.\n"
                + "Entrada: " + DateFormats.format(r.checkIn()) + " | Saída: " + DateFormats.format(r.checkOut()) + "\n"
                + "Hóspedes: " + r.guests();
        return List.of(Message.email(ADDRESS, "Reserva confirmada - quarto " + room.number(), body));
    }

    @Override
    public List<Message> onPaidReservationCancelled(Reservation r, Room room) {
        String body = "A reserva " + r.id() + " do quarto " + room.number() + " foi cancelada.\n"
                + "Datas que ficaram livres: de " + DateFormats.format(r.checkIn())
                + " a " + DateFormats.format(r.checkOut()) + ".";
        return List.of(Message.email(ADDRESS, "Reserva cancelada - quarto " + room.number(), body));
    }
}
