package br.com.marealta.pousada.domain;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static br.com.marealta.pousada.domain.DomainException.Kind.*;

public class ReservationService {

    private final ReservationRepository repository;
    private final RoomCatalog rooms;
    private final List<ReservationListener> listeners;
    private final ReentrantLock lock = new ReentrantLock();

    public ReservationService(ReservationRepository repository, RoomCatalog rooms,
                              List<ReservationListener> listeners) {
        this.repository = repository;
        this.rooms = rooms;
        this.listeners = List.copyOf(listeners);
    }

    public Reservation create(NewReservation request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new DomainException(INVALID_REQUEST, "a saída deve ser depois da entrada");
        }
        if (request.guests() < 1) {
            throw new DomainException(INVALID_REQUEST, "é preciso ao menos um hóspede");
        }
        Room room = rooms.find(request.roomNumber())
                .orElseThrow(() -> new DomainException(ROOM_NOT_FOUND, "quarto inexistente"));
        if (request.guests() > room.capacity()) {
            throw new DomainException(CAPACITY_EXCEEDED, "o quarto comporta no máximo " + room.capacity());
        }
        lock.lock();
        try {
            if (repository.existsOverlap(room.number(), request.checkIn(), request.checkOut())) {
                throw new DomainException(ROOM_UNAVAILABLE, "quarto já reservado em alguma das noites");
            }
            return repository.insert(Reservation.pending(0, room, request.guest(), request.checkIn(),
                    request.checkOut(), request.guests(), request.ratePlan()));
        } finally {
            lock.unlock();
        }
    }

    public Reservation get(long id) {
        return repository.find(id)
                .orElseThrow(() -> new DomainException(RESERVATION_NOT_FOUND, "reserva inexistente"));
    }

    public Reservation confirm(long id) {
        Reservation reservation = change(id, Reservation::confirm);
        listeners.forEach(l -> l.onConfirmed(reservation));
        return reservation;
    }

    public Reservation cancel(long id) {
        ReservationStatus[] previous = new ReservationStatus[1];
        Reservation reservation = change(id, r -> {
            previous[0] = r.status();
            r.cancel();
        });
        listeners.forEach(l -> l.onCancelled(reservation, previous[0]));
        return reservation;
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::startStay);
    }

    public Receipt checkOut(long id) {
        return Receipt.of(change(id, Reservation::endStay));
    }

    /** Segunda via do comprovante: só existe para reserva paga. */
    public Receipt document(long id) {
        Reservation reservation = get(id);
        if (!reservation.status().isPaid()) {
            throw new DomainException(OPERATION_NOT_ALLOWED, "a reserva não tem pagamento");
        }
        return Receipt.of(reservation);
    }

    public List<Reservation> all() {
        return repository.findAll();
    }

    private Reservation change(long id, java.util.function.Consumer<Reservation> action) {
        lock.lock();
        try {
            Reservation reservation = get(id);
            action.accept(reservation);
            repository.update(reservation);
            return reservation;
        } finally {
            lock.unlock();
        }
    }
}
