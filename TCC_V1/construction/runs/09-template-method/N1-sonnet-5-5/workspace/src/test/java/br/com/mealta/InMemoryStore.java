package br.com.mealta;

import br.com.mealta.domain.Message;
import br.com.mealta.domain.Reservation;
import br.com.mealta.store.MessageLog;
import br.com.mealta.store.ReservationRepository;
import br.com.mealta.store.Transactor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Implementação em memória das portas de armazenamento, só para os testes. */
public class InMemoryStore implements ReservationRepository, MessageLog, Transactor {
    private final List<Reservation> reservations = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();

    @Override
    public synchronized Reservation save(Reservation r) {
        if (r.id() == null) {
            r.assignId(reservations.size() + 1);
            reservations.add(r);
        }
        return r;
    }

    @Override
    public synchronized Optional<Reservation> findById(long id) {
        return reservations.stream().filter(r -> r.id() == id).findFirst();
    }

    @Override
    public Optional<Reservation> lockById(long id) {
        return findById(id);
    }

    @Override
    public void lockRoom(int roomNumber) {
    }

    @Override
    public synchronized boolean hasActiveOverlap(int room, LocalDate in, LocalDate out) {
        return reservations.stream().anyMatch(r -> r.roomNumber() == room
                && r.status() != br.com.mealta.domain.ReservationStatus.CANCELLED
                && r.checkInDate().isBefore(out) && r.checkOutDate().isAfter(in));
    }

    @Override
    public synchronized List<Reservation> startingIn(YearMonth month) {
        return reservations.stream().filter(r -> YearMonth.from(r.checkInDate()).equals(month))
                .sorted(Comparator.comparing(Reservation::checkInDate).thenComparing(Reservation::id)).toList();
    }

    @Override
    public synchronized List<Reservation> paidOccupying(LocalDate night) {
        return reservations.stream().filter(r -> r.isPaid() && r.occupies(night))
                .sorted(Comparator.comparingInt(Reservation::roomNumber)).toList();
    }

    @Override
    public synchronized Message add(Message m) {
        Message saved = new Message((long) messages.size() + 1, m.channel(), m.to(), m.subject(), m.body());
        messages.add(saved);
        return saved;
    }

    @Override
    public synchronized List<Message> all() {
        return List.copyOf(messages);
    }

    @Override
    public <T> T run(Supplier<T> work) {
        return work.get();
    }
}
