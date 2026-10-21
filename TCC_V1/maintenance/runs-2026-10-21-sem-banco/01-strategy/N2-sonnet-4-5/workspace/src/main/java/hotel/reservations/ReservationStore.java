package hotel.reservations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Keeps the reservations in memory: everything is lost when the service restarts. */
public final class ReservationStore {

    private final Map<Long, Reservation> reservationsById = new LinkedHashMap<>();
    private long lastId = 0;

    synchronized long nextId() {
        lastId++;
        return lastId;
    }

    synchronized void save(Reservation reservation) {
        reservationsById.put(reservation.id(), reservation);
    }

    public synchronized Optional<Reservation> find(long id) {
        return Optional.ofNullable(reservationsById.get(id));
    }

    public synchronized List<Reservation> all() {
        return new ArrayList<>(reservationsById.values());
    }
}
