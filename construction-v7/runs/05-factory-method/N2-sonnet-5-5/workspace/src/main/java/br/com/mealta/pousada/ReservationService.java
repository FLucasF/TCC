package br.com.mealta.pousada;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReservationService {
    private final Map<Integer, Room> rooms = new TreeMap<>();
    private final Map<Long, Reservation> reservations = new TreeMap<>();
    private final Outbox outbox;

    public ReservationService(Outbox outbox) {
        this.outbox = outbox;
        for (Room r : List.of(new Room(101, RoomType.STANDARD), new Room(102, RoomType.STANDARD),
                new Room(201, RoomType.DELUXE), new Room(202, RoomType.DELUXE),
                new Room(301, RoomType.SUITE))) {
            rooms.put(r.number(), r);
        }
    }

    public List<Room> rooms() {
        return new ArrayList<>(rooms.values());
    }

    public synchronized Reservation create(int roomNumber, String guestName, String guestEmail, Identity identity,
                                           LocalDate checkIn, LocalDate checkOut, int guests, RatePlan plan) {
        Room room = rooms.get(roomNumber);
        if (room == null) {
            throw new DomainException(404, "ROOM_NOT_FOUND", "Quarto não encontrado: " + roomNumber);
        }
        if (guests > room.capacity()) {
            throw new DomainException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + roomNumber + " comporta até " + room.capacity() + " pessoas");
        }
        boolean taken = reservations.values().stream()
                .anyMatch(r -> r.room().number() == roomNumber
                        && r.status() != ReservationStatus.CANCELLED
                        && r.overlaps(checkIn, checkOut));
        if (taken) {
            throw new DomainException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
        }
        long id = reservations.size() + 1L;
        Reservation reservation = new Reservation(id, room, guestName, guestEmail, identity, checkIn, checkOut,
                guests, plan, Pricing.quote(room, checkIn, checkOut, plan));
        reservations.put(id, reservation);
        return reservation;
    }

    public synchronized Reservation get(long id) {
        Reservation r = reservations.get(id);
        if (r == null) {
            throw new DomainException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada: " + id);
        }
        return r;
    }

    public synchronized Reservation confirm(long id) {
        Reservation r = get(id);
        r.confirm();
        outbox.sendEmail(r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", Documents.confirmationEmail(r));
        return r;
    }

    public synchronized Reservation cancel(long id) {
        Reservation r = get(id);
        r.cancel();
        return r;
    }

    public synchronized Reservation checkIn(long id) {
        Reservation r = get(id);
        r.startStay();
        return r;
    }

    public synchronized Reservation checkOut(long id) {
        Reservation r = get(id);
        r.finishStay();
        return r;
    }

    /** Segunda via: só para reserva paga. */
    public synchronized Reservation paidReservation(long id) {
        Reservation r = get(id);
        if (!r.status().isPaid()) {
            throw DomainException.notAllowed("A reserva ainda não foi paga");
        }
        return r;
    }

    public synchronized List<Reservation> all() {
        return new ArrayList<>(reservations.values());
    }
}
