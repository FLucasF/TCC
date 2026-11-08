package br.com.marealta.pousada;

import java.time.LocalDate;
import java.util.*;

import br.com.marealta.pousada.domain.*;
import br.com.marealta.pousada.notification.*;

final class InMemory {

    static final class Reservations implements ReservationRepository {
        private final Map<Long, Reservation> data = new TreeMap<>();

        public Reservation insert(Reservation draft) {
            Reservation saved = draft.withId(data.size() + 1);
            data.put(saved.id(), saved);
            return saved;
        }
        public void update(Reservation r) { data.put(r.id(), r); }
        public Optional<Reservation> find(long id) { return Optional.ofNullable(data.get(id)); }
        public List<Reservation> findAll() { return new ArrayList<>(data.values()); }
        public boolean existsOverlap(int room, LocalDate in, LocalDate out) {
            return data.values().stream().anyMatch(r -> r.room().number() == room
                    && r.status() != ReservationStatus.CANCELLED
                    && r.checkIn().isBefore(out) && r.checkOut().isAfter(in));
        }
    }

    static final class Messages implements MessageLog {
        final List<Message> sent = new ArrayList<>();

        public Message send(Channel channel, String to, String subject, String body) {
            Message m = new Message(sent.size() + 1, channel, to, subject, body);
            sent.add(m);
            return m;
        }
        public List<Message> all() { return sent; }
    }

    static List<ReservationListener> listeners(MessageLog log) {
        return List.of(new GuestConfirmationNotice(log),
                new HousekeepingNotice(log, "governanca@pousadamarealta.com.br"),
                new FinanceNotice(log, "financeiro@pousadamarealta.com.br"));
    }
}
