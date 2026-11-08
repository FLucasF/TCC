package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;

import java.util.List;
import java.util.function.Function;

/** Reúne as mensagens que todos os interessados querem mandar para cada acontecimento. */
public class ReservationEvents {

    private final List<ReservationListener> listeners;

    public ReservationEvents(List<ReservationListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    public List<Message> confirmed(Reservation reservation, Room room) {
        return collect(l -> l.onConfirmed(reservation, room));
    }

    public List<Message> paidReservationCancelled(Reservation reservation, Room room) {
        return collect(l -> l.onPaidReservationCancelled(reservation, room));
    }

    private List<Message> collect(Function<ReservationListener, List<Message>> reaction) {
        return listeners.stream().flatMap(l -> reaction.apply(l).stream()).toList();
    }
}
