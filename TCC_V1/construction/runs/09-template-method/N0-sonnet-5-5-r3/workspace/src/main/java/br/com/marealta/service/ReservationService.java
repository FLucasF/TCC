package br.com.marealta.service;

import br.com.marealta.db.Database;
import br.com.marealta.db.MessageRepository;
import br.com.marealta.db.ReservationRepository;
import br.com.marealta.db.RoomRepository;
import br.com.marealta.domain.Channel;
import br.com.marealta.domain.Message;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class ReservationService {

    public static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final Database db;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    public ReservationService(Database db, RoomRepository rooms, ReservationRepository reservations,
                              MessageRepository messages) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
    }

    public List<Room> listRooms() {
        return db.transaction(rooms::findAll);
    }

    public List<Message> listMessages() {
        return db.transaction(messages::findAll);
    }

    public Reservation create(BookingRequest req) {
        return db.transaction(c -> {
            Room room = rooms.find(c, req.roomNumber()).orElseThrow(() -> ApiException.roomNotFound(req.roomNumber()));
            if (req.guests() > room.capacity()) throw ApiException.capacityExceeded(room.capacity());
            rooms.lock(c, room.number());
            if (reservations.hasOverlap(c, room.number(), req.checkIn(), req.checkOut())) {
                throw ApiException.roomUnavailable();
            }
            Quote quote = Quote.of(room, req.checkIn(), req.checkOut(), req.ratePlan());
            Reservation draft = new Reservation(0, room.number(), req.guestName(), req.guestEmail(), req.guestCpf(),
                    req.checkIn(), req.checkOut(), req.guests(), req.ratePlan(), quote.nights(), quote.subtotal(),
                    quote.discount(), quote.total(), ReservationStatus.PENDING, Money.ZERO, null);
            return reservations.insert(c, draft);
        });
    }

    public Reservation get(String id) {
        return db.transaction(c -> load(c, id, false));
    }

    public Reservation confirm(String id) {
        return db.transaction(c -> {
            Reservation r = load(c, id, true);
            require(r, r.status() == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
            Reservation confirmed = r.confirmed();
            reservations.updateState(c, confirmed);
            Room room = rooms.find(c, r.roomNumber()).orElseThrow();
            messages.add(c, Channel.EMAIL, r.guestEmail(), CONFIRMATION_SUBJECT, confirmationBody(r, room));
            return confirmed;
        });
    }

    public Reservation cancel(String id) {
        return transition(id, r -> r.status() == ReservationStatus.PENDING || r.status() == ReservationStatus.CONFIRMED,
                "Só é possível cancelar uma reserva pendente ou confirmada antes do check-in", Reservation::cancelled);
    }

    public Reservation checkIn(String id) {
        return transition(id, r -> r.status() == ReservationStatus.CONFIRMED,
                "Só é possível fazer check-in de uma reserva confirmada", r -> r.withStatus(ReservationStatus.CHECKED_IN));
    }

    public Receipt checkOut(String id) {
        return db.transaction(c -> {
            Reservation r = load(c, id, true);
            require(r, r.status() == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out após o check-in");
            Reservation done = r.withStatus(ReservationStatus.CHECKED_OUT);
            reservations.updateState(c, done);
            return Receipt.of(done, rooms.find(c, r.roomNumber()).orElseThrow());
        });
    }

    /** Second copy of the receipt; only exists for paid reservations. */
    public Receipt document(String id) {
        return db.transaction(c -> {
            Reservation r = load(c, id, false);
            require(r, r.isPaid(), "Só existe comprovante para reserva paga");
            return Receipt.of(r, rooms.find(c, r.roomNumber()).orElseThrow());
        });
    }

    private interface Rule {
        boolean allows(Reservation r);
    }

    private Reservation transition(String id, Rule rule, String refusal,
                                   java.util.function.UnaryOperator<Reservation> change) {
        return db.transaction(c -> {
            Reservation r = load(c, id, true);
            require(r, rule.allows(r), refusal);
            Reservation next = change.apply(r);
            reservations.updateState(c, next);
            return next;
        });
    }

    private static void require(Reservation r, boolean ok, String message) {
        if (!ok) throw ApiException.notAllowed(message + " (situação atual: " + r.status() + ")");
    }

    private Reservation load(Connection c, String id, boolean forUpdate) throws SQLException {
        long parsed;
        try {
            parsed = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(id);
        }
        return reservations.find(c, parsed, forUpdate).orElseThrow(() -> ApiException.reservationNotFound(id));
    }

    static String confirmationBody(Reservation r, Room room) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(r, room).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
