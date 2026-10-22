package pousada;

import java.time.LocalDate;
import java.util.List;

public final class ReservationService {
    private final Database db;

    public ReservationService(Database db) {
        this.db = db;
    }

    public record NewReservation(int roomNumber, String guestName, String guestEmail, GuestIdentity identity,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    public Reservation create(NewReservation n) {
        Room room = Room.find(n.roomNumber())
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado: " + n.roomNumber()));
        if (n.guests() > room.type().capacity) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + room.number() + " comporta até " + room.type().capacity + " pessoas");
        }
        Quote quote = Quote.of(room.type(), n.checkIn(), n.checkOut(), n.ratePlan());
        return db.inTransaction(c -> {
            Store.lockRoom(c, room.number());
            if (Store.isRoomBusy(c, room.number(), n.checkIn(), n.checkOut())) {
                throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
            }
            return Store.insert(c, new Reservation(0, room.number(), n.guestName(), n.guestEmail(), n.identity(),
                    n.checkIn(), n.checkOut(), n.guests(), n.ratePlan(), quote.nights(), quote.subtotal(),
                    quote.discount(), quote.total(), ReservationStatus.PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(long id) {
        return db.inTransaction(c -> Store.find(c, id, false).orElseThrow(() -> notFound(id)));
    }

    public Reservation confirm(long id) {
        return db.inTransaction(c -> {
            Reservation r = require(c, id, ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
            Reservation paid = with(r, ReservationStatus.CONFIRMED, r.total(), r.refund());
            Store.updateState(c, paid);
            Store.insertMessage(c, "EMAIL", paid.guestEmail(), Documents.confirmationSubject(),
                    Documents.confirmationBody(paid, Documents.issue(paid)));
            return paid;
        });
    }

    public Reservation cancel(long id) {
        return db.inTransaction(c -> {
            Reservation r = Store.find(c, id, true).orElseThrow(() -> notFound(id));
            if (r.status() != ReservationStatus.PENDING && r.status() != ReservationStatus.CONFIRMED) {
                throw ApiException.notAllowed("Só é possível cancelar uma reserva pendente ou confirmada");
            }
            // A pending reservation has paid nothing, so its refund is zero either way.
            var refund = r.ratePlan().refundable ? r.amountPaid() : Money.ZERO;
            Reservation cancelled = with(r, ReservationStatus.CANCELLED, r.amountPaid(), refund);
            Store.updateState(c, cancelled);
            return cancelled;
        });
    }

    public Reservation checkIn(long id) {
        return db.inTransaction(c -> {
            Reservation r = require(c, id, ReservationStatus.CONFIRMED,
                    "Só é possível fazer o check-in de uma reserva confirmada");
            Reservation updated = with(r, ReservationStatus.CHECKED_IN, r.amountPaid(), r.refund());
            Store.updateState(c, updated);
            return updated;
        });
    }

    public BookingDocument checkOut(long id) {
        return db.inTransaction(c -> {
            Reservation r = require(c, id, ReservationStatus.CHECKED_IN,
                    "Só é possível fazer o check-out de quem fez o check-in");
            Reservation updated = with(r, ReservationStatus.CHECKED_OUT, r.amountPaid(), r.refund());
            Store.updateState(c, updated);
            return Documents.issue(updated);
        });
    }

    /** Second copy of the document; only exists for paid reservations. */
    public BookingDocument document(long id) {
        Reservation r = get(id);
        if (!r.status().isPaid()) {
            throw ApiException.notAllowed("A segunda via só existe para reserva paga");
        }
        return Documents.issue(r);
    }

    public List<Message> messages() {
        return db.inTransaction(Store::messages);
    }

    public List<Reservation> occupying(LocalDate night) {
        return db.inTransaction(c -> Store.occupying(c, night));
    }

    public List<Reservation> startingBetween(LocalDate from, LocalDate to) {
        return db.inTransaction(c -> Store.startingBetween(c, from, to));
    }

    private Reservation require(java.sql.Connection c, long id, ReservationStatus expected, String message)
            throws java.sql.SQLException {
        Reservation r = Store.find(c, id, true).orElseThrow(() -> notFound(id));
        if (r.status() != expected) throw ApiException.notAllowed(message);
        return r;
    }

    private static Reservation with(Reservation r, ReservationStatus status, java.math.BigDecimal paid,
                                    java.math.BigDecimal refund) {
        return new Reservation(r.id(), r.roomNumber(), r.guestName(), r.guestEmail(), r.identity(), r.checkIn(),
                r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(), r.total(),
                status, paid, refund);
    }

    private static ApiException notFound(long id) {
        return new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada: " + id);
    }
}
