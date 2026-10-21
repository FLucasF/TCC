package pousada.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import pousada.domain.Reservation;

public class ReservationRepository {
    private final ConcurrentHashMap<Integer, Reservation> byId = new ConcurrentHashMap<>();
    private final AtomicInteger lastId = new AtomicInteger();

    public int nextId() {
        return lastId.incrementAndGet();
    }

    public void save(Reservation reservation) {
        byId.put(reservation.id(), reservation);
    }

    public Optional<Reservation> find(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<Reservation> all() {
        return List.copyOf(byId.values());
    }
}
