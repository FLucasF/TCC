package pousada.repo;

import pousada.domain.Reservation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import pousada.domain.ReservationStatus;

public class InMemoryReservationRepository implements ReservationRepository {
    private final Map<Long, Reservation> data = new TreeMap<>();
    private long nextId = 1;

    @Override
    public synchronized Reservation insert(Reservation reservation) {
        reservation.assignId(nextId++);
        data.put(reservation.id(), reservation);
        return reservation;
    }

    @Override
    public synchronized void update(Reservation reservation) {
        data.put(reservation.id(), reservation);
    }

    @Override
    public synchronized Optional<Reservation> find(long id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public synchronized boolean hasOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return data.values().stream().anyMatch(r -> r.roomNumber() == roomNumber
                && r.status() != ReservationStatus.CANCELLED
                && r.checkIn().isBefore(checkOut) && r.checkOut().isAfter(checkIn));
    }

    @Override
    public synchronized List<Reservation> findCheckInBetween(LocalDate from, LocalDate to) {
        return data.values().stream()
                .filter(r -> !r.checkIn().isBefore(from) && !r.checkIn().isAfter(to))
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparing(Reservation::id))
                .toList();
    }

    @Override
    public synchronized List<Reservation> findPaidOccupying(LocalDate night) {
        List<Reservation> found = new ArrayList<>(data.values().stream()
                .filter(r -> r.status().isPaid() && r.occupies(night)).toList());
        found.sort(Comparator.comparing(Reservation::roomNumber).thenComparing(Reservation::id));
        return found;
    }
}
