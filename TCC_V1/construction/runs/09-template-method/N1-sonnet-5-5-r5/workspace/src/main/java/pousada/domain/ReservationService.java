package pousada.domain;

import java.util.function.UnaryOperator;

import pousada.db.Database;
import pousada.db.ReservationRepository;
import pousada.db.RoomRepository;
import pousada.error.ApiException;

public class ReservationService {
    private final Database db;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final Notifier notifier;

    public ReservationService(Database db, RoomRepository rooms, ReservationRepository reservations, Notifier notifier) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
        this.notifier = notifier;
    }

    public Reservation create(NewReservation request) {
        return db.transaction(() -> {
            Room room = rooms.lock(request.roomNumber()).orElseThrow(ApiException::roomNotFound);
            if (request.guests() > room.capacity()) {
                throw ApiException.capacityExceeded();
            }
            if (reservations.existsOverlap(room.number(), request.checkIn(), request.checkOut())) {
                throw ApiException.roomUnavailable();
            }
            return reservations.insert(request,
                    Pricing.quote(room, request.checkIn(), request.checkOut(), request.ratePlan()));
        });
    }

    public Reservation get(long id) {
        return reservations.find(id).orElseThrow(ApiException::reservationNotFound);
    }

    public Reservation confirm(long id) {
        return db.transaction(() -> {
            Reservation confirmed = change(id, Reservation::afterConfirmation);
            notifier.send(Channel.EMAIL, confirmed.guestEmail(), ConfirmationEmail.SUBJECT,
                    ConfirmationEmail.body(receiptOf(confirmed)));
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return change(id, Reservation::afterCancellation);
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::afterCheckIn);
    }

    public Receipt checkOut(long id) {
        return db.transaction(() -> receiptOf(change(id, Reservation::afterCheckOut)));
    }

    public Receipt document(long id) {
        Reservation reservation = get(id);
        reservation.requirePaid();
        return receiptOf(reservation);
    }

    /** Trava a reserva, aplica a mudança de situação e grava. */
    private Reservation change(long id, UnaryOperator<Reservation> transition) {
        return db.transaction(() -> {
            Reservation current = reservations.lock(id).orElseThrow(ApiException::reservationNotFound);
            Reservation updated = transition.apply(current);
            reservations.save(updated);
            return updated;
        });
    }

    private Receipt receiptOf(Reservation reservation) {
        Room room = rooms.find(reservation.roomNumber()).orElseThrow(ApiException::roomNotFound);
        return Receipt.of(reservation, room);
    }
}
