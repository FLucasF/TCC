package br.com.marealta;

import br.com.marealta.domain.*;
import br.com.marealta.store.Store;
import br.com.marealta.store.Tx;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/** Test double: no rollback, so the services must only write after all checks pass. */
public final class InMemoryStore implements Store, Tx {
    private final List<Room> rooms = List.of(
            new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")));
    private final Map<Long, Reservation> reservations = new TreeMap<>();
    private final List<Message> messages = new ArrayList<>();

    @Override
    public synchronized <T> T inTransaction(Function<Tx, T> work) {
        return work.apply(this);
    }

    public List<Room> rooms() {
        return rooms;
    }

    public Optional<Room> room(int number) {
        return rooms.stream().filter(r -> r.number() == number).findFirst();
    }

    public void lockRoom(int number) {
    }

    public boolean roomBooked(int room, LocalDate in, LocalDate out) {
        return reservations.values().stream().anyMatch(r -> r.roomNumber() == room && r.holdsRoom()
                && r.checkIn().isBefore(out) && in.isBefore(r.checkOut()));
    }

    public Reservation insert(Reservation r) {
        Reservation saved = new Reservation(reservations.size() + 1L, r.roomNumber(), r.guestName(),
                r.guestEmail(), r.guestCpf(), r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.nights(),
                r.subtotal(), r.discount(), r.total(), r.status(), r.amountPaid(), r.refund());
        reservations.put(saved.id(), saved);
        return saved;
    }

    public void update(Reservation r) {
        reservations.put(r.id(), r);
    }

    public Optional<Reservation> reservation(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public Optional<Reservation> reservationForUpdate(long id) {
        return reservation(id);
    }

    public List<Reservation> reservationsCheckingInBetween(LocalDate from, LocalDate to) {
        return reservations.values().stream()
                .filter(r -> !r.checkIn().isBefore(from) && r.checkIn().isBefore(to)).toList();
    }

    public List<Reservation> reservationsCoveringNight(LocalDate night) {
        return reservations.values().stream()
                .filter(r -> !r.checkIn().isAfter(night) && night.isBefore(r.checkOut())).toList();
    }

    public Message saveMessage(Message m) {
        Message saved = new Message(messages.size() + 1L, m.channel(), m.to(), m.subject(), m.body());
        messages.add(saved);
        return saved;
    }

    public List<Message> messages() {
        return List.copyOf(messages);
    }
}
