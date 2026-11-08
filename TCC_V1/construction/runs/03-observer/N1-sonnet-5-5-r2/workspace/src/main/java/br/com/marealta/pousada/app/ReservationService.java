package br.com.marealta.pousada.app;

import br.com.marealta.pousada.domain.ConfirmedReservationCancelled;
import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.PousadaException;
import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationConfirmed;
import br.com.marealta.pousada.domain.ReservationStatus;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.notification.EventBus;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.persistence.ReservationRepository;

import java.util.function.Consumer;

public class ReservationService {

    private final Database db;
    private final ReservationRepository reservations;
    private final EventBus events;

    public ReservationService(Database db, ReservationRepository reservations, EventBus events) {
        this.db = db;
        this.reservations = reservations;
        this.events = events;
    }

    public Reservation create(BookingRequest request) {
        Room room = Room.find(request.roomNumber()).orElseThrow(() ->
                new PousadaException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + request.roomNumber() + " não existe"));
        Reservation draft = Reservation.open(room, request.guestName(), request.guestEmail(), request.guestCpf(),
                request.checkIn(), request.checkOut(), request.guests(), request.ratePlan());
        return db.transaction(() -> {
            reservations.lockRoom(room.number());
            if (reservations.isRoomBooked(room.number(), request.checkIn(), request.checkOut())) {
                throw new PousadaException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
            }
            return reservations.insert(draft);
        });
    }

    public Reservation find(long id) {
        return db.transaction(() -> load(id));
    }

    public Reservation confirm(long id) {
        return change(id, Reservation::confirm, ReservationConfirmed::new);
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::registerCheckIn, null);
    }

    public Reservation cancel(long id) {
        return db.transaction(() -> {
            Reservation r = load(id);
            boolean wasConfirmed = r.status() == ReservationStatus.CONFIRMED;
            r.cancel();
            reservations.update(r);
            if (wasConfirmed) {
                events.publish(new ConfirmedReservationCancelled(r));
            }
            return r;
        });
    }

    public Receipt checkOut(long id) {
        return change(id, Reservation::registerCheckOut, null).receipt();
    }

    public Receipt document(long id) {
        return db.transaction(() -> load(id).receipt());
    }

    /** A sequência comum das transições: carregar, mudar, gravar e, se houver, publicar o acontecimento. */
    private Reservation change(long id, Consumer<Reservation> transition,
                               java.util.function.Function<Reservation, Object> event) {
        return db.transaction(() -> {
            Reservation r = load(id);
            transition.accept(r);
            reservations.update(r);
            if (event != null) {
                events.publish(event.apply(r));
            }
            return r;
        });
    }

    private Reservation load(long id) {
        return reservations.findById(id).orElseThrow(() ->
                new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe"));
    }
}
