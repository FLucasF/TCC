package br.com.marealta.store;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.Status;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Armazenamento em memória, usado nos testes. */
public class MemoryStore implements Store {
    private final List<Room> rooms;
    private final List<Reservation> reservations = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();

    public MemoryStore(List<Room> rooms) {
        this.rooms = List.copyOf(rooms);
    }

    @Override
    public synchronized <T> T tx(Function<Tx, T> work) {
        return work.apply(new MemoryTx());
    }

    private final class MemoryTx implements Tx {
        @Override
        public List<Room> rooms() { return rooms; }

        @Override
        public Optional<Room> room(int number, boolean lock) {
            return rooms.stream().filter(r -> r.number() == number).findFirst();
        }

        @Override
        public boolean overlaps(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
            return reservations.stream().anyMatch(r -> r.roomNumber() == roomNumber
                    && r.status() != Status.CANCELLED
                    && r.checkIn().isBefore(checkOut) && checkIn.isBefore(r.checkOut()));
        }

        @Override
        public Reservation insert(Reservation reservation) {
            Reservation saved = reservation.withId(reservations.size() + 1L);
            reservations.add(saved);
            return saved;
        }

        @Override
        public Optional<Reservation> reservation(long id, boolean lock) {
            return reservations.stream().filter(r -> r.id() == id).findFirst();
        }

        @Override
        public void update(Reservation reservation) {
            reservations.replaceAll(r -> r.id() == reservation.id() ? reservation : r);
        }

        @Override
        public Message addMessage(String channel, String to, String subject, String body) {
            Message m = new Message(messages.size() + 1L, channel, to, subject, body);
            messages.add(m);
            return m;
        }

        @Override
        public List<Message> messages() { return List.copyOf(messages); }

        @Override
        public List<Reservation> startingBetween(LocalDate from, LocalDate toExclusive) {
            return reservations.stream()
                    .filter(r -> !r.checkIn().isBefore(from) && r.checkIn().isBefore(toExclusive))
                    .sorted(Comparator.comparing(Reservation::checkIn).thenComparing(Reservation::id))
                    .toList();
        }

        @Override
        public List<Reservation> stayingOn(LocalDate night) {
            return reservations.stream()
                    .filter(r -> !r.checkIn().isAfter(night) && night.isBefore(r.checkOut()))
                    .sorted(Comparator.comparingInt(Reservation::roomNumber).thenComparing(Reservation::id))
                    .toList();
        }
    }
}
