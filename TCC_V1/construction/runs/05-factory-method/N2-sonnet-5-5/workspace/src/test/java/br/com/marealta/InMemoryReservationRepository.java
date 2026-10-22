package br.com.marealta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class InMemoryReservationRepository implements ReservationRepository {
    private final List<Reservation> items = new ArrayList<>();

    public synchronized void insert(Reservation r) {
        r.assignId(items.size() + 1L);
        items.add(r);
    }

    public synchronized void update(Reservation r) {}

    public synchronized Optional<Reservation> find(long id) {
        return items.stream().filter(r -> r.id() == id).findFirst();
    }

    public synchronized List<Reservation> findAll() { return new ArrayList<>(items); }
}
