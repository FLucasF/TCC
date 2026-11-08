package br.com.marealta.app;

import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.PousadaException;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.notification.ReservationListener;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

public final class ReservationService {

    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
    }

    private final Database db;
    private final RoomCatalog rooms;
    private final ReservationRepository repository;
    private final List<ReservationListener> listeners;

    public ReservationService(Database db, RoomCatalog rooms, ReservationRepository repository,
                              List<ReservationListener> listeners) {
        this.db = db;
        this.rooms = rooms;
        this.repository = repository;
        this.listeners = List.copyOf(listeners);
    }

    public Reservation create(NewReservation request) {
        Room room = rooms.require(request.roomNumber());
        Reservation reservation = Reservation.create(room, request.guestName(), request.guestEmail(),
                request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(), request.ratePlan());
        return db.transaction(() -> {
            repository.lockRoom(room.number());
            if (repository.existsOverlapping(room.number(), request.checkIn(), request.checkOut())) {
                throw new PousadaException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
            }
            repository.insert(reservation);
            return reservation;
        });
    }

    public Reservation get(long id) {
        return repository.find(id).orElseThrow(() -> notFound(id));
    }

    public Reservation confirm(long id) {
        return change(id, Reservation::confirm,
                reservation -> listeners.forEach(l -> l.onConfirmed(reservation)));
    }

    public Reservation cancel(long id) {
        return change(id, Reservation::cancel,
                reservation -> listeners.forEach(l -> l.onCancelled(reservation)));
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::checkIn, reservation -> { });
    }

    public Receipt checkOut(long id) {
        return change(id, Reservation::checkOut, reservation -> { }).receipt();
    }

    public Receipt document(long id) {
        return get(id).receipt();
    }

    /** Muda a reserva, grava e avisa os ouvintes, tudo na mesma transação. */
    private Reservation change(long id, Consumer<Reservation> change, Consumer<Reservation> announce) {
        return db.transaction(() -> {
            Reservation reservation = repository.findForUpdate(id).orElseThrow(() -> notFound(id));
            change.accept(reservation);
            repository.update(reservation);
            announce.accept(reservation);
            return reservation;
        });
    }

    private static PousadaException notFound(long id) {
        return new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "A reserva " + id + " não existe");
    }
}
