package br.com.marealta.service;

import br.com.marealta.domain.Reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Reservas em memória. Quem precisa de consistência entre várias operações sincroniza no próprio repositório. */
public class ReservationRepository {
    private final List<Reservation> reservations = new ArrayList<>();

    public synchronized int nextId() { return reservations.size() + 1; }

    public synchronized void add(Reservation reservation) { reservations.add(reservation); }

    public synchronized Optional<Reservation> find(int id) {
        return id >= 1 && id <= reservations.size() ? Optional.of(reservations.get(id - 1)) : Optional.empty();
    }

    public synchronized List<Reservation> all() { return List.copyOf(reservations); }
}
