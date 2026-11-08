package pousada;

import pousada.domain.AppException;
import pousada.domain.ErrorCode;
import pousada.domain.Message;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.service.ReservationRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class InMemoryRepository implements ReservationRepository {
    private final List<Reservation> reservations = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();

    @Override
    public synchronized Reservation insert(Reservation draft) {
        boolean taken = reservations.stream().anyMatch(r -> r.room().equals(draft.room())
                && r.status() != ReservationStatus.CANCELLED
                && r.checkInDate().isBefore(draft.checkOutDate()) && r.checkOutDate().isAfter(draft.checkInDate()));
        if (taken) {
            throw new AppException(ErrorCode.ROOM_UNAVAILABLE, "ocupado");
        }
        Reservation saved = draft.withId(reservations.size() + 1);
        reservations.add(saved);
        return saved;
    }

    @Override
    public synchronized Optional<Reservation> find(long id) {
        return reservations.stream().filter(r -> r.id() == id).findFirst();
    }

    @Override
    public synchronized void update(Reservation reservation, ReservationStatus expected, List<Message> out) {
        for (Message m : out) {
            messages.add(new Message((long) messages.size() + 1, m.channel(), m.to(), m.subject(), m.body()));
        }
    }

    @Override
    public synchronized List<Message> messages() {
        return List.copyOf(messages);
    }

    @Override
    public synchronized List<Reservation> paidOnNight(LocalDate night) {
        return reservations.stream().filter(r -> r.status().isPaid()
                && !r.checkInDate().isAfter(night) && r.checkOutDate().isAfter(night)).toList();
    }

    @Override
    public synchronized List<Reservation> checkInBetween(LocalDate from, LocalDate to) {
        return reservations.stream()
                .filter(r -> !r.checkInDate().isBefore(from) && !r.checkInDate().isAfter(to)).toList();
    }
}
