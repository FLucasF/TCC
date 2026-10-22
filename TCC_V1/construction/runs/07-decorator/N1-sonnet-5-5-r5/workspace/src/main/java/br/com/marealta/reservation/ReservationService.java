package br.com.marealta.reservation;

import br.com.marealta.Money;
import br.com.marealta.db.Database;
import br.com.marealta.message.MessageDispatcher;
import br.com.marealta.web.ApiException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.UnaryOperator;

public class ReservationService {

    private final Database database;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MessageDispatcher messages;

    public ReservationService(Database database, RoomRepository rooms, ReservationRepository reservations,
                              MessageDispatcher messages) {
        this.database = database;
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
    }

    public List<Room> rooms() {
        return database.run(rooms::all);
    }

    public Reservation create(NewReservation request) {
        return database.run(connection -> {
            Room room = rooms.findForUpdate(connection, request.roomNumber())
                    .orElseThrow(() -> ApiException.roomNotFound(request.roomNumber()));
            if (request.guests() > room.capacity()) {
                throw ApiException.capacityExceeded(room.capacity());
            }
            if (reservations.overlaps(connection, room.number(), request.checkIn(), request.checkOut())) {
                throw ApiException.roomUnavailable(room.number());
            }
            Pricing.Quote quote = Pricing.quote(room.nightlyRate(), request.checkIn(), request.checkOut(),
                    request.ratePlan());
            return reservations.insert(connection, new Reservation(null, room.number(), request.guestName(),
                    request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(),
                    request.guests(), request.ratePlan(), quote.nights(), quote.subtotal(), quote.discount(),
                    quote.total(), Status.PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(String id) {
        return database.run(connection -> load(connection, id, false));
    }

    public Reservation confirm(String id) {
        return database.run(connection -> {
            Reservation confirmed = change(connection, id, Reservation::confirm);
            Room room = rooms.find(connection, confirmed.roomNumber()).orElseThrow();
            messages.send(connection, ConfirmationEmail.of(confirmed, Receipt.of(confirmed, room)));
            return confirmed;
        });
    }

    public Reservation cancel(String id) {
        return database.run(connection -> change(connection, id, Reservation::cancel));
    }

    public Reservation checkIn(String id) {
        return database.run(connection -> change(connection, id, Reservation::checkInGuest));
    }

    public Receipt checkOut(String id) {
        return database.run(connection -> receipt(connection, change(connection, id, Reservation::checkOutGuest)));
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(String id) {
        return database.run(connection -> {
            Reservation reservation = load(connection, id, false);
            if (!reservation.status().isPaid()) {
                throw ApiException.operationNotAllowed("Só reserva paga tem comprovante");
            }
            return receipt(connection, reservation);
        });
    }

    private Reservation change(Connection connection, String id, UnaryOperator<Reservation> transition)
            throws SQLException {
        Reservation updated = transition.apply(load(connection, id, true));
        reservations.update(connection, updated);
        return updated;
    }

    private Receipt receipt(Connection connection, Reservation reservation) throws SQLException {
        return Receipt.of(reservation, rooms.find(connection, reservation.roomNumber()).orElseThrow());
    }

    private Reservation load(Connection connection, String id, boolean forUpdate) throws SQLException {
        long parsed;
        try {
            parsed = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(id);
        }
        return reservations.find(connection, parsed, forUpdate)
                .orElseThrow(() -> ApiException.reservationNotFound(id));
    }
}
