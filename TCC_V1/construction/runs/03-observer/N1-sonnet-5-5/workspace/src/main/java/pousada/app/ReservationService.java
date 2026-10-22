package pousada.app;

import pousada.domain.*;
import pousada.infra.Database;
import pousada.infra.ReservationRepository;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ReservationService {
    private final Database db;
    private final ReservationRepository repository;
    private final RoomCatalog rooms;
    private final EventBus events;

    public ReservationService(Database db, ReservationRepository repository, RoomCatalog rooms, EventBus events) {
        this.db = db;
        this.repository = repository;
        this.rooms = rooms;
        this.events = events;
    }

    public Reservation create(NewReservation request) {
        Room room = rooms.find(request.roomNumber()).orElseThrow(() ->
                new DomainException(DomainException.Code.ROOM_NOT_FOUND, "Quarto " + request.roomNumber() + " não existe"));
        Reservation reservation = Reservation.open(room, request.guest(), request.checkIn(), request.checkOut(),
                request.guests(), request.ratePlan());
        return db.inTransaction(c -> {
            repository.lockRoom(c, room);
            if (repository.existsOverlapping(c, room, request.checkIn(), request.checkOut())) {
                throw new DomainException(DomainException.Code.ROOM_UNAVAILABLE,
                        "Quarto " + room.number() + " já reservado em alguma das noites pedidas");
            }
            return repository.insert(c, reservation);
        });
    }

    public Reservation get(long id) {
        return db.inTransaction(c -> repository.find(c, id, false).orElseThrow(() -> notFound(id)));
    }

    public Reservation confirm(long id) {
        Reservation reservation = change(id, Reservation::confirm);
        events.publish(new ReservationConfirmed(reservation));
        return reservation;
    }

    public Reservation cancel(long id) {
        AtomicReference<ReservationStatus> before = new AtomicReference<>();
        Reservation reservation = change(id, r -> {
            before.set(r.status());
            r.cancel();
        });
        if (before.get() == ReservationStatus.CONFIRMED) {
            events.publish(new ConfirmedReservationCancelled(reservation));
        }
        return reservation;
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::startStay);
    }

    public Document checkOut(long id) {
        return Document.of(change(id, Reservation::endStay));
    }

    public Document document(long id) {
        return Document.of(get(id));
    }

    private Reservation change(long id, Consumer<Reservation> operation) {
        return db.inTransaction(c -> {
            Reservation reservation = repository.find(c, id, true).orElseThrow(() -> notFound(id));
            operation.accept(reservation);
            repository.save(c, reservation);
            return reservation;
        });
    }

    private static DomainException notFound(long id) {
        return new DomainException(DomainException.Code.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }
}
