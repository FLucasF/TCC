package br.com.marealta;

import br.com.marealta.app.Message;
import br.com.marealta.app.MessageRepository;
import br.com.marealta.app.Notification;
import br.com.marealta.app.ReservationRepository;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class InMemoryRepositories {
    static class Reservations implements ReservationRepository {
        private final List<Reservation> items = new ArrayList<>();

        public Reservation insert(Reservation r) {
            Reservation saved = r.withId(items.size() + 1);
            items.add(saved);
            return saved;
        }

        public void update(Reservation r) {
            items.set((int) (r.id() - 1), r);
        }

        public Optional<Reservation> find(long id) {
            return id >= 1 && id <= items.size() ? Optional.of(items.get((int) id - 1)) : Optional.empty();
        }

        public List<Reservation> findAll() {
            return List.copyOf(items);
        }

        public boolean existsActiveOverlap(int room, LocalDate in, LocalDate out) {
            return items.stream().anyMatch(r -> r.roomNumber() == room && r.status() != ReservationStatus.CANCELLED
                    && r.checkIn().isBefore(out) && in.isBefore(r.checkOut()));
        }
    }

    static class Messages implements MessageRepository {
        private final List<Message> items = new ArrayList<>();

        public Message save(Notification n) {
            Message m = new Message(items.size() + 1, n.channel(), n.to(), n.subject(), n.body());
            items.add(m);
            return m;
        }

        public List<Message> findAll() {
            return List.copyOf(items);
        }
    }
}
