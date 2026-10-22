package br.com.marealta.service;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.Message;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.Stay;
import br.com.marealta.infra.Db;
import br.com.marealta.infra.MessageRepository;
import br.com.marealta.infra.ReservationRepository;
import br.com.marealta.plan.RatePlan;
import br.com.marealta.plan.RatePlans;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ReservationService {

    @FunctionalInterface
    private interface Operation<T> {
        T run(Connection connection, Reservation reservation) throws SQLException;
    }

    private final Db db;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    public ReservationService(Db db, ReservationRepository reservations, MessageRepository messages) {
        this.db = db;
        this.reservations = reservations;
        this.messages = messages;
    }

    public Reservation create(NewReservation request) {
        Room room = Room.find(request.roomNumber()).orElseThrow(() -> ApiException.roomNotFound(request.roomNumber()));
        if (request.guests() > room.capacity()) {
            throw ApiException.capacityExceeded(room.capacity());
        }
        RatePlan plan = RatePlans.get(request.ratePlan());
        Stay stay = new Stay(room, request.checkIn(), request.checkOut(), request.companyCode());
        Reservation pending = Reservation.pending(room, request.guestName(), request.guestEmail(),
                request.guestCpf(), stay, request.guests(), plan, plan.quote(stay));

        return db.transaction(connection -> {
            reservations.lockRoom(connection, room.number());
            if (reservations.isRoomBooked(connection, room.number(), stay.checkIn(), stay.checkOut())) {
                throw ApiException.roomUnavailable(room.number());
            }
            return reservations.insert(connection, pending);
        });
    }

    public Reservation get(String id) {
        long parsed = parseId(id);
        return db.transaction(connection -> reservations.find(connection, parsed)
                .orElseThrow(() -> ApiException.reservationNotFound(id)));
    }

    public Reservation confirm(String id) {
        return change(id, (connection, reservation) -> {
            Reservation confirmed = reservation.confirm();
            reservations.update(connection, confirmed);
            messages.add(connection, ConfirmationEmail.of(confirmed));
            return confirmed;
        });
    }

    public Reservation cancel(String id) {
        return change(id, (connection, reservation) -> {
            Reservation cancelled = reservation.cancel(RatePlans.get(reservation.ratePlan()));
            reservations.update(connection, cancelled);
            return cancelled;
        });
    }

    public Reservation checkIn(String id) {
        return change(id, (connection, reservation) -> {
            Reservation checkedIn = reservation.startStay();
            reservations.update(connection, checkedIn);
            return checkedIn;
        });
    }

    public Receipt checkOut(String id) {
        return change(id, (connection, reservation) -> {
            Reservation checkedOut = reservation.endStay();
            reservations.update(connection, checkedOut);
            return Receipt.of(checkedOut);
        });
    }

    /** Segunda via do comprovante, só para reserva paga. */
    public Receipt receiptCopy(String id) {
        Reservation reservation = get(id);
        if (!reservation.isPaid()) {
            throw ApiException.operationNotAllowed("Só reservas pagas têm comprovante");
        }
        return Receipt.of(reservation);
    }

    public List<Message> sentMessages() {
        return db.transaction(messages::findAll);
    }

    /** Roda a operação com a reserva travada, para duas operações na mesma reserva não se atropelarem. */
    private <T> T change(String id, Operation<T> operation) {
        long parsed = parseId(id);
        return db.transaction(connection -> {
            Reservation reservation = reservations.findForUpdate(connection, parsed)
                    .orElseThrow(() -> ApiException.reservationNotFound(id));
            return operation.run(connection, reservation);
        });
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(id);
        }
    }
}
