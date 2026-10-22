package br.com.maralta.pousada;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.UnaryOperator;

public class ReservationService {
    private static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final Database database;
    private final ReservationRepository reservations = new ReservationRepository();
    private final MessageRepository messages = new MessageRepository();

    public ReservationService(Database database) {
        this.database = database;
    }

    public Reservation create(ReservationRequest request) {
        Room room = Rooms.get(request.roomNumber());
        if (request.guests() > room.capacity()) {
            throw ApiException.capacityExceeded(room.capacity());
        }
        Pricing.Quote quote = Pricing.quote(room, request.checkIn(), request.checkOut(), request.ratePlan());
        Reservation draft = new Reservation(0, room.number(), request.guestName(), request.guestEmail(),
                request.identification(), request.checkIn(), request.checkOut(), request.guests(),
                request.ratePlan(), quote.nights(), quote.subtotal(), quote.discount(), quote.total(),
                Status.PENDING, Money.ZERO, null);
        return database.inTransaction(c -> {
            reservations.lockRoom(c, room.number());
            if (reservations.isRoomBooked(c, room.number(), request.checkIn(), request.checkOut())) {
                throw ApiException.roomUnavailable(room.number());
            }
            return reservations.insert(c, draft);
        });
    }

    public Reservation get(String id) {
        return database.inTransaction(c -> load(c, id, false));
    }

    public Reservation confirm(String id) {
        return transition(id, Reservation::confirm, (c, r) -> messages.send(c, Channel.EMAIL, r.guestEmail(),
                CONFIRMATION_SUBJECT, confirmationBody(r)));
    }

    public Reservation cancel(String id) {
        return transition(id, Reservation::cancel, (c, r) -> { });
    }

    public Reservation checkIn(String id) {
        return transition(id, Reservation::checkInGuest, (c, r) -> { });
    }

    public Document checkOut(String id) {
        return Document.of(transition(id, Reservation::checkOutGuest, (c, r) -> { }));
    }

    public Document secondCopy(String id) {
        Reservation reservation = get(id);
        reservation.requirePaid();
        return Document.of(reservation);
    }

    public List<Message> messages() {
        return database.inTransaction(messages::findAll);
    }

    @FunctionalInterface
    private interface AfterChange {
        void run(Connection c, Reservation changed) throws SQLException;
    }

    /** A sequência comum das operações: trava a reserva, aplica a mudança, grava e executa o que vem depois. */
    private Reservation transition(String id, UnaryOperator<Reservation> change, AfterChange after) {
        return database.inTransaction(c -> {
            Reservation changed = change.apply(load(c, id, true));
            reservations.update(c, changed);
            after.run(c, changed);
            return changed;
        });
    }

    private Reservation load(Connection c, String id, boolean forUpdate) throws SQLException {
        long number;
        try {
            number = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(id);
        }
        return reservations.find(c, number, forUpdate).orElseThrow(() -> ApiException.reservationNotFound(id));
    }

    private static String confirmationBody(Reservation reservation) {
        return "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Document.of(reservation).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
