package br.com.marealta.reservas.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final RoomCatalog rooms;
    private final List<ReservationListener> listeners;
    private final List<Reservation> reservations = new ArrayList<>();

    public ReservationService(RoomCatalog rooms, List<ReservationListener> listeners) {
        this.rooms = rooms;
        this.listeners = List.copyOf(listeners);
    }

    public synchronized Reservation create(NewReservation request) {
        Room room = rooms.find(request.roomNumber())
                .orElseThrow(() -> DomainException.roomNotFound(request.roomNumber()));
        if (request.guests() > room.capacity()) throw DomainException.capacityExceeded(room.capacity());
        boolean taken = reservations.stream().anyMatch(r -> r.overlaps(room, request.checkIn(), request.checkOut()));
        if (taken) throw DomainException.roomUnavailable(room.number());
        Reservation reservation = new Reservation(reservations.size() + 1, room, request);
        reservations.add(reservation);
        return reservation;
    }

    public synchronized Reservation confirm(String id) {
        Reservation reservation = get(id);
        reservation.confirm();
        listeners.forEach(l -> l.onConfirmed(reservation));
        return reservation;
    }

    public synchronized Reservation cancel(String id) {
        Reservation reservation = get(id);
        ReservationStatus previous = reservation.status();
        reservation.cancel();
        listeners.forEach(l -> l.onCancelled(reservation, previous));
        return reservation;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation reservation = get(id);
        reservation.startStay();
        return reservation;
    }

    public synchronized Reservation checkOut(String id) {
        Reservation reservation = get(id);
        reservation.endStay();
        return reservation;
    }

    /** Second copy of the receipt; only exists for paid reservations. */
    public synchronized Reservation paidReservation(String id) {
        Reservation reservation = get(id);
        if (!reservation.status().isPaid()) {
            throw DomainException.operationNotAllowed("Comprovante só existe para reserva paga");
        }
        return reservation;
    }

    public synchronized Reservation get(String id) {
        return find(id).orElseThrow(() -> DomainException.reservationNotFound(id));
    }

    public synchronized List<Reservation> all() {
        return List.copyOf(reservations);
    }

    private Optional<Reservation> find(String id) {
        try {
            int number = Integer.parseInt(id);
            return number >= 1 && number <= reservations.size()
                    ? Optional.of(reservations.get(number - 1)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
