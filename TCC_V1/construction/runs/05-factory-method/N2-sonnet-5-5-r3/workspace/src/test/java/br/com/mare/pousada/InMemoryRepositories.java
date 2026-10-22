package br.com.mare.pousada;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class InMemoryRepositories {
    private InMemoryRepositories() {
    }

    static class Reservations implements ReservationRepository {
        private final List<Reservation> items = new ArrayList<>();

        @Override
        public Reservation insert(Reservation r) {
            Reservation saved = r.withId(items.size() + 1);
            items.add(saved);
            return saved;
        }

        @Override
        public void update(Reservation r) {
        }

        @Override
        public Optional<Reservation> find(int id) {
            return items.stream().filter(r -> r.id() == id).findFirst();
        }

        @Override
        public List<Reservation> findAll() {
            return List.copyOf(items);
        }
    }

    static class Messages implements MessageRepository {
        private final List<Message> items = new ArrayList<>();

        @Override
        public Message add(Channel channel, String to, String subject, String body) {
            Message m = new Message(items.size() + 1, channel, to, subject, body);
            items.add(m);
            return m;
        }

        @Override
        public List<Message> findAll() {
            return List.copyOf(items);
        }
    }
}
