package pousada.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.UnaryOperator;
import pousada.ApiException;
import pousada.document.Document;
import pousada.domain.Message;
import pousada.domain.Money;
import pousada.domain.Pricing;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.domain.Room;
import pousada.domain.Rooms;
import pousada.persistence.Database;
import pousada.persistence.MessageRepository;
import pousada.persistence.ReservationRepository;

public final class ReservationService {
    private final Database db;
    private final ReservationRepository reservations = new ReservationRepository();
    private final MessageRepository messages = new MessageRepository();

    public ReservationService(Database db) {
        this.db = db;
    }

    public Reservation create(NewReservation req) {
        Room room = Rooms.find(req.roomNumber())
                .orElseThrow(() -> new ApiException(ApiException.Code.ROOM_NOT_FOUND,
                        "Quarto " + req.roomNumber() + " não existe"));
        if (req.guests() > room.capacity()) {
            throw new ApiException(ApiException.Code.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        Pricing.Quote quote = Pricing.quote(room, req.checkIn(), req.checkOut(), req.ratePlan());
        Reservation pending = new Reservation(0, room.number(), req.guestName(), req.guestEmail(),
                req.identity(), req.checkIn(), req.checkOut(), req.guests(), req.ratePlan(), quote.nights(),
                quote.subtotal(), quote.discount(), quote.total(), ReservationStatus.PENDING, Money.ZERO, null);
        return db.tx(c -> {
            reservations.lockRoom(c, room.number());
            if (reservations.isRoomTaken(c, room.number(), req.checkIn(), req.checkOut())) {
                throw new ApiException(ApiException.Code.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma dessas noites");
            }
            return reservations.insert(c, pending);
        });
    }

    public Reservation get(long id) {
        return db.tx(c -> load(c, id, false));
    }

    public Reservation confirm(long id) {
        return db.tx(c -> {
            Reservation confirmed = change(c, id, Reservation::confirm);
            messages.save(c, ConfirmationEmail.of(confirmed, Document.issue(confirmed)));
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return db.tx(c -> change(c, id, Reservation::cancel));
    }

    public Reservation checkIn(long id) {
        return db.tx(c -> change(c, id, Reservation::startStay));
    }

    public Document checkOut(long id) {
        return db.tx(c -> Document.issue(change(c, id, Reservation::endStay)));
    }

    /** Segunda via: só existe para reserva paga. */
    public Document document(long id) {
        Reservation r = get(id);
        if (!r.isPaid()) {
            throw ApiException.notAllowed("Reserva " + id + " está " + r.status() + " e não tem documento");
        }
        return Document.issue(r);
    }

    public List<Message> sentMessages() {
        return db.tx(messages::findAll);
    }

    private Reservation change(Connection c, long id, UnaryOperator<Reservation> operation) throws SQLException {
        Reservation changed = operation.apply(load(c, id, true));
        reservations.update(c, changed);
        return changed;
    }

    private Reservation load(Connection c, long id, boolean lock) throws SQLException {
        return (lock ? reservations.findForUpdate(c, id) : reservations.find(c, id))
                .orElseThrow(() -> new ApiException(ApiException.Code.RESERVATION_NOT_FOUND,
                        "Reserva " + id + " não existe"));
    }
}
