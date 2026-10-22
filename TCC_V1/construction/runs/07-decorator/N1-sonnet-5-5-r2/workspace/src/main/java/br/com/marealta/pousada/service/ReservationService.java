package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.ApiException;
import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.Pricing;
import br.com.marealta.pousada.domain.Quote;
import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Status;
import br.com.marealta.pousada.store.Database;
import br.com.marealta.pousada.store.ReservationRepository;
import br.com.marealta.pousada.store.RoomRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.function.UnaryOperator;

public class ReservationService {
    private final Database database;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final Outbox outbox;

    public ReservationService(Database database, RoomRepository rooms, ReservationRepository reservations,
                              Outbox outbox) {
        this.database = database;
        this.rooms = rooms;
        this.reservations = reservations;
        this.outbox = outbox;
    }

    public List<Room> listRooms() {
        return database.inTransaction(rooms::findAll);
    }

    public Reservation create(NewReservation request) {
        return database.inTransaction(c -> {
            Room room = rooms.findForUpdate(c, request.roomNumber()).orElseThrow(() ->
                    new ApiException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + request.roomNumber() + " não existe"));
            if (request.guests() > room.capacity()) {
                throw new ApiException(ErrorCode.CAPACITY_EXCEEDED,
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " pessoas");
            }
            if (reservations.existsOverlapping(c, room.number(), request.checkIn(), request.checkOut())) {
                throw new ApiException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
            }
            Quote quote = Pricing.quote(room, request.checkIn(), request.checkOut(), request.ratePlan());
            Reservation draft = new Reservation(0, room.number(), request.guestName(), request.guestEmail(),
                    request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(), request.ratePlan(),
                    quote.nights(), quote.subtotal(), quote.discount(), quote.total(), Status.PENDING,
                    Money.ZERO, null);
            return reservations.insert(c, draft);
        });
    }

    public Reservation get(long id) {
        return database.inTransaction(c -> load(c, id));
    }

    public Reservation confirm(long id) {
        Reservation confirmed = transition(id, Reservation::confirm);
        outbox.send(ConfirmationEmail.compose(confirmed, receiptOf(confirmed)));
        return confirmed;
    }

    public Reservation cancel(long id) {
        return transition(id, Reservation::cancel);
    }

    public Reservation checkIn(long id) {
        return transition(id, Reservation::startStay);
    }

    public Receipt checkOut(long id) {
        return receiptOf(transition(id, Reservation::endStay));
    }

    /** Segunda via do comprovante: só existe para reserva paga. */
    public Receipt document(long id) {
        Reservation reservation = get(id);
        if (!reservation.status().paid()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A reserva " + id + " está " + reservation.status() + " e não tem comprovante");
        }
        return receiptOf(reservation);
    }

    private Reservation transition(long id, UnaryOperator<Reservation> change) {
        return database.inTransaction(c -> {
            Reservation changed = change.apply(reservations.findForUpdate(c, id).orElseThrow(() -> notFound(id)));
            reservations.update(c, changed);
            return changed;
        });
    }

    private Receipt receiptOf(Reservation reservation) {
        return database.inTransaction(c -> {
            Room room = rooms.find(c, reservation.roomNumber()).orElseThrow();
            return Receipt.of(reservation, room);
        });
    }

    private Reservation load(java.sql.Connection c, long id) throws SQLException {
        return reservations.find(c, id).orElseThrow(() -> notFound(id));
    }

    private ApiException notFound(long id) {
        return new ApiException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }
}
