package br.com.marealta.store;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/** Implementação em memória, usada nos testes. Uma transação por vez. */
public class MemoryStore implements Store, Tx {
    private final Map<Integer, Room> rooms = new TreeMap<>();
    private final Map<Long, Reservation> reservations = new TreeMap<>();
    private final List<Message> messages = new ArrayList<>();
    private long nextReservationId = 1;
    private long nextMessageId = 1;

    public MemoryStore(List<Room> catalog) {
        catalog.forEach(r -> rooms.put(r.number(), r));
    }

    @Override
    public synchronized <T> T transaction(Function<Tx, T> work) {
        return work.apply(this);
    }

    @Override public List<Room> rooms() { return List.copyOf(rooms.values()); }

    @Override public Optional<Room> room(int number) { return Optional.ofNullable(rooms.get(number)); }

    @Override public void lockRoom(int number) {}

    @Override
    public boolean hasActiveOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.values().stream().anyMatch(r -> r.roomNumber() == roomNumber
                && r.status() != ReservationStatus.CANCELLED
                && r.checkIn().isBefore(checkOut) && r.checkOut().isAfter(checkIn));
    }

    @Override
    public Reservation insert(Reservation draft) {
        Reservation saved = new Reservation(nextReservationId++, draft.roomNumber(), draft.guestName(),
                draft.guestEmail(), draft.guestCpf(), draft.checkIn(), draft.checkOut(), draft.guests(),
                draft.ratePlan(), draft.nights(), draft.subtotal(), draft.discount(), draft.total(),
                draft.status(), draft.amountPaid(), draft.refund());
        reservations.put(saved.id(), saved);
        return saved;
    }

    @Override public Optional<Reservation> reservationForUpdate(long id) { return reservation(id); }

    @Override public Optional<Reservation> reservation(long id) { return Optional.ofNullable(reservations.get(id)); }

    @Override public void update(Reservation reservation) { reservations.put(reservation.id(), reservation); }

    @Override
    public List<Reservation> startingBetween(LocalDate from, LocalDate to) {
        return reservations.values().stream()
                .filter(r -> !r.checkIn().isBefore(from) && !r.checkIn().isAfter(to))
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparing(Reservation::id))
                .toList();
    }

    @Override
    public List<Reservation> occupying(LocalDate night) {
        return reservations.values().stream()
                .filter(r -> !r.checkIn().isAfter(night) && r.checkOut().isAfter(night))
                .sorted(Comparator.comparingInt(Reservation::roomNumber).thenComparing(Reservation::id))
                .toList();
    }

    @Override
    public Message addMessage(String channel, String to, String subject, String body) {
        Message m = new Message(nextMessageId++, channel, to, subject, body);
        messages.add(m);
        return m;
    }

    @Override public List<Message> messages() { return List.copyOf(messages); }
}
