package br.com.marealta.store;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.event.Message;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/** Implementação em memória, para testes. */
public class MemoryStore implements Store {
    private final TreeMap<Long, Reservation> reservations = new TreeMap<>();
    private final List<Message> messages = new ArrayList<>();

    @Override
    public synchronized <T> T tx(Function<Tx, T> work) {
        // Sem rollback: o serviço só grava depois de validar tudo.
        return work.apply(new MemTx());
    }

    private class MemTx implements Tx {
        public List<Room> rooms() { return Room.ALL; }
        public Optional<Room> room(int n) { return Room.ALL.stream().filter(r -> r.number() == n).findFirst(); }
        public void lockRoom(int number) {}

        public boolean hasOverlap(int room, LocalDate in, LocalDate out) {
            return reservations.values().stream().anyMatch(r -> r.roomNumber() == room
                    && r.status() != ReservationStatus.CANCELLED
                    && r.checkIn().isBefore(out) && r.checkOut().isAfter(in));
        }

        public Reservation insert(Reservation r) {
            long id = reservations.isEmpty() ? 1 : reservations.lastKey() + 1;
            Reservation saved = new Reservation(id, r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(),
                    r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(),
                    r.total(), r.status(), r.amountPaid(), r.refund());
            reservations.put(id, saved);
            return saved;
        }

        public Optional<Reservation> find(long id) { return Optional.ofNullable(reservations.get(id)); }
        public Optional<Reservation> findForUpdate(long id) { return find(id); }
        public void update(Reservation r) { reservations.put(r.id(), r); }
        public List<Reservation> all() { return new ArrayList<>(reservations.values()); }

        public void addMessage(Message m) {
            messages.add(new Message(messages.size() + 1L, m.channel(), m.to(), m.subject(), m.body()));
        }

        public List<Message> messages() { return new ArrayList<>(messages); }
    }
}
