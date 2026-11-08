package br.com.marealta;

import br.com.marealta.plan.Quote;
import java.sql.SQLException;
import java.util.List;

public final class ReservationService {
    private final Database db;
    private final ReservationRepository reservations = new ReservationRepository();
    private final MessageRepository messages = new MessageRepository();

    public ReservationService(Database db) {
        this.db = db;
    }

    public Reservation create(NewReservation request) {
        Room room = Rooms.find(request.roomNumber()).orElseThrow(() -> ApiError.roomNotFound(request.roomNumber()));
        Quote quote = request.plan().quote(new Stay(room, request.checkIn(), request.checkOut(), request.companyCode()));
        if (request.guests() > room.capacity()) {
            throw ApiError.capacityExceeded(room.capacity());
        }
        return db.inTransaction(c -> {
            reservations.lockRoom(c, room.number());
            if (reservations.hasConflict(c, room.number(), request.checkIn(), request.checkOut())) {
                throw ApiError.roomUnavailable();
            }
            return reservations.insert(c, request, quote);
        });
    }

    public Reservation get(String id) {
        return db.read(c -> reservations.find(c, parseId(id), false)
                .orElseThrow(() -> ApiError.reservationNotFound(id)));
    }

    public Reservation confirm(String id) {
        return db.inTransaction(c -> {
            Reservation confirmed = lock(c, id).confirm();
            reservations.update(c, confirmed);
            messages.record(c, "EMAIL", confirmed.guestEmail(), ConfirmationEmail.SUBJECT,
                    ConfirmationEmail.body(confirmed));
            return confirmed;
        });
    }

    public Reservation cancel(String id) {
        return db.inTransaction(c -> {
            Reservation cancelled = lock(c, id).cancel();
            reservations.update(c, cancelled);
            return cancelled;
        });
    }

    public Reservation checkIn(String id) {
        return db.inTransaction(c -> {
            Reservation r = lock(c, id).startStay();
            reservations.update(c, r);
            return r;
        });
    }

    public Receipt checkOut(String id) {
        return db.inTransaction(c -> {
            Reservation r = lock(c, id).endStay();
            reservations.update(c, r);
            return Receipt.of(r);
        });
    }

    public Receipt document(String id) {
        Reservation r = get(id);
        r.requirePaid();
        return Receipt.of(r);
    }

    public List<Message> messages() {
        return db.read(messages::all);
    }

    private Reservation lock(java.sql.Connection c, String id) throws SQLException {
        return reservations.find(c, parseId(id), true).orElseThrow(() -> ApiError.reservationNotFound(id));
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw ApiError.reservationNotFound(id);
        }
    }
}
