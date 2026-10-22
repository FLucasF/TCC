package br.com.marealta.notify;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.event.DomainEvent;
import br.com.marealta.event.DomainEvent.ReservationCancelled;
import br.com.marealta.event.DomainEvent.ReservationConfirmed;
import br.com.marealta.event.Message;
import br.com.marealta.event.Subscriber;

import java.util.List;

/** Avisa a governança para preparar (ou liberar) o quarto. */
public class HousekeepingNotifier implements Subscriber {
    public static final String ADDRESS = "governanca@pousadamarealta.com.br";

    @Override
    public List<Message> on(DomainEvent event) {
        Reservation r = event.reservation();
        int room = event.room().number();
        String dates = "Entrada: " + Money.date(r.checkIn()) + " | Saída: " + Money.date(r.checkOut());
        return switch (event) {
            case ReservationConfirmed e -> List.of(Message.email(ADDRESS, "Reserva confirmada - quarto " + room,
                    "O quarto " + room + " foi reservado (reserva " + r.id() + ").\n"
                            + dates + "\nHóspedes: " + r.guests()));
            case ReservationCancelled e when e.wasConfirmed() -> List.of(Message.email(ADDRESS,
                    "Reserva cancelada - quarto " + room,
                    "A reserva " + r.id() + " do quarto " + room + " foi cancelada. Datas liberadas:\n" + dates));
            case ReservationCancelled e -> List.of();
        };
    }
}
