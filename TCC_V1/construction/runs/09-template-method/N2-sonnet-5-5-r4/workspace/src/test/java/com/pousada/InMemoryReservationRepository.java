package com.pousada;

import com.pousada.domain.DomainException;
import com.pousada.domain.ErrorCode;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.repository.ReservationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

class InMemoryReservationRepository implements ReservationRepository {
    private final List<Reservation> all = new ArrayList<>();

    @Override
    public synchronized Reservation create(Reservation draft) {
        boolean taken = all.stream().anyMatch(r -> r.getRoomNumber() == draft.getRoomNumber()
                && r.getStatus() != ReservationStatus.CANCELLED
                && r.getCheckIn().isBefore(draft.getCheckOut()) && r.getCheckOut().isAfter(draft.getCheckIn()));
        if (taken) {
            throw new DomainException(ErrorCode.ROOM_UNAVAILABLE, "ocupado");
        }
        Reservation saved = draft.withId(all.size() + 1);
        all.add(saved);
        return saved;
    }

    @Override
    public synchronized Optional<Reservation> find(long id) {
        return all.stream().filter(r -> r.getId() == id).findFirst();
    }

    @Override
    public synchronized Reservation update(long id, Consumer<Reservation> change) {
        Reservation r = find(id).orElseThrow(() -> new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "não existe"));
        change.accept(r);
        return r;
    }

    @Override
    public synchronized List<Reservation> findAll() {
        return List.copyOf(all);
    }
}
