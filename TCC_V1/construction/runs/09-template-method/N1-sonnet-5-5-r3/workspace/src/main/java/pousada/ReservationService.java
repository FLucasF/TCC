package pousada;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.UnaryOperator;

public final class ReservationService {
    private final Db db;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    public ReservationService(Db db, RoomRepository rooms, ReservationRepository reservations, MessageRepository messages) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
    }

    public List<Room> rooms() {
        return db.tx(rooms::all);
    }

    public List<Message> messages() {
        return db.tx(messages::all);
    }

    public Reservation create(ReservationRequest request) {
        return db.tx(c -> {
            Room room = rooms.findForUpdate(c, request.roomNumber())
                    .orElseThrow(() -> ApiException.roomNotFound(request.roomNumber()));
            if (request.guests() > room.capacity()) {
                throw ApiException.capacityExceeded(room.capacity());
            }
            if (reservations.hasOverlap(c, room.number(), request.checkIn(), request.checkOut())) {
                throw ApiException.roomUnavailable();
            }
            Pricing.Quote quote = Pricing.quote(room, request.checkIn(), request.checkOut(), request.ratePlan());
            return reservations.insert(c, new Reservation(null, room.number(), request.guestName(),
                    request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(),
                    request.ratePlan(), quote.nights(), quote.subtotal(), quote.discount(), quote.total(),
                    ReservationStatus.PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(String id) {
        return db.tx(c -> load(c, id, false));
    }

    public Reservation confirm(String id) {
        return db.tx(c -> {
            Reservation confirmed = apply(c, id, Reservation::confirm);
            Receipt receipt = receiptOf(c, confirmed);
            messages.add(c, Message.confirmation(confirmed, receipt));
            return confirmed;
        });
    }

    public Reservation cancel(String id) {
        return db.tx(c -> apply(c, id, Reservation::cancel));
    }

    public Reservation checkIn(String id) {
        return db.tx(c -> apply(c, id, Reservation::registerCheckIn));
    }

    public Receipt checkOut(String id) {
        return db.tx(c -> receiptOf(c, apply(c, id, Reservation::registerCheckOut)));
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(String id) {
        return db.tx(c -> {
            Reservation r = load(c, id, false);
            if (!r.status().isPaidStay()) {
                throw ApiException.operationNotAllowed("Só há comprovante para reserva paga");
            }
            return receiptOf(c, r);
        });
    }

    private Reservation apply(Connection c, String id, UnaryOperator<Reservation> change) throws SQLException {
        Reservation changed = change.apply(load(c, id, true));
        reservations.update(c, changed);
        return changed;
    }

    private Reservation load(Connection c, String id, boolean lock) throws SQLException {
        long parsed;
        try {
            parsed = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(id);
        }
        return reservations.find(c, parsed, lock).orElseThrow(() -> ApiException.reservationNotFound(id));
    }

    private Receipt receiptOf(Connection c, Reservation r) throws SQLException {
        Room room = rooms.find(c, r.roomNumber()).orElseThrow(() -> ApiException.roomNotFound(r.roomNumber()));
        return Receipt.of(r, room);
    }
}
