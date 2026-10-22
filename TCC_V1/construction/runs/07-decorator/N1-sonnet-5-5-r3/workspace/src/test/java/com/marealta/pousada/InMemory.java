package com.marealta.pousada;

import com.marealta.pousada.domain.Reservation;
import com.marealta.pousada.mail.Message;
import com.marealta.pousada.persistence.MessageRepository;
import com.marealta.pousada.persistence.ReservationRepository;

import java.time.LocalDate;
import java.util.*;

final class InMemory {
    private InMemory() {
    }

    static final class Reservations implements ReservationRepository {
        private final Map<Long, Reservation> byId = new TreeMap<>();

        public long insert(Reservation r) {
            long id = byId.size() + 1;
            byId.put(id, r);
            return id;
        }

        public void update(Reservation r) {
        }

        public Optional<Reservation> find(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        public List<Reservation> findActiveOverlapping(int room, LocalDate in, LocalDate out) {
            return byId.values().stream().filter(r -> r.roomNumber() == room
                    && r.status() != com.marealta.pousada.domain.ReservationStatus.CANCELLED
                    && r.checkIn().isBefore(out) && r.checkOut().isAfter(in)).toList();
        }

        public List<Reservation> findOverlapping(LocalDate in, LocalDate out) {
            return byId.values().stream().filter(r -> r.checkIn().isBefore(out) && r.checkOut().isAfter(in)).toList();
        }

        public List<Reservation> findByCheckInBetween(LocalDate from, LocalDate to) {
            return byId.values().stream().filter(r -> !r.checkIn().isBefore(from) && !r.checkIn().isAfter(to)).toList();
        }
    }

    static final class Messages implements MessageRepository {
        private final List<Message> all = new ArrayList<>();

        public void save(Message m) {
            all.add(m.withId(all.size() + 1));
        }

        public List<Message> findAll() {
            return List.copyOf(all);
        }
    }
}
