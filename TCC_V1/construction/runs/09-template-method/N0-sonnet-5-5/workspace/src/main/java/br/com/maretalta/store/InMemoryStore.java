package br.com.maretalta.store;

import br.com.maretalta.domain.Message;
import br.com.maretalta.domain.Reservation;
import br.com.maretalta.domain.Status;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/** Implementação em memória (testes). Uma transação por vez. */
public class InMemoryStore implements Store {
    private final TreeMap<Long, Reservation> reservations = new TreeMap<>();
    private final List<Message> messages = new ArrayList<>();

    @Override
    public synchronized <T> T inTransaction(Function<Tx, T> work) {
        return work.apply(new MemTx());
    }

    private class MemTx implements Tx {
        @Override public void lockRoom(int roomNumber) {}

        @Override
        public boolean hasOverlap(int room, LocalDate in, LocalDate out) {
            return reservations.values().stream().anyMatch(r -> r.roomNumber() == room
                    && r.status() != Status.CANCELLED
                    && r.checkIn().isBefore(out) && in.isBefore(r.checkOut()));
        }

        @Override
        public Reservation insert(Reservation draft) {
            Reservation saved = draft.withId(reservations.isEmpty() ? 1 : reservations.lastKey() + 1);
            reservations.put(saved.id(), saved);
            return saved;
        }

        @Override public Optional<Reservation> findForUpdate(long id) { return find(id); }

        @Override public Optional<Reservation> find(long id) { return Optional.ofNullable(reservations.get(id)); }

        @Override public void update(Reservation r) { reservations.put(r.id(), r); }

        @Override
        public List<Reservation> startingIn(YearMonth month) {
            return reservations.values().stream()
                    .filter(r -> YearMonth.from(r.checkIn()).equals(month))
                    .sorted(Comparator.comparing(Reservation::checkIn).thenComparing(Reservation::id))
                    .toList();
        }

        @Override
        public List<Reservation> paidOccupying(LocalDate night) {
            return reservations.values().stream()
                    .filter(r -> r.status().isPaid() && !r.checkIn().isAfter(night) && night.isBefore(r.checkOut()))
                    .sorted(Comparator.comparing(Reservation::roomNumber).thenComparing(Reservation::id))
                    .toList();
        }

        @Override
        public Message addMessage(String channel, String to, String subject, String body) {
            Message m = new Message(messages.size() + 1L, channel, to, subject, body);
            messages.add(m);
            return m;
        }

        @Override public List<Message> messages() { return List.copyOf(messages); }
    }
}
