package br.com.marealta.service;

import br.com.marealta.domain.BusinessException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Message;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.Rooms;
import br.com.marealta.domain.Stay;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.MessageRepository;
import br.com.marealta.persistence.ReservationRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public class ReservationService {
    private final Database database;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    public ReservationService(Database database, ReservationRepository reservations, MessageRepository messages) {
        this.database = database;
        this.reservations = reservations;
        this.messages = messages;
    }

    public Reservation create(BookingRequest request) {
        Room room = Rooms.find(request.roomNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND,
                        "Quarto não encontrado: " + request.roomNumber()));
        if (request.guests() > room.capacity()) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta até " + room.capacity() + " hóspedes");
        }
        var quote = request.ratePlan().quote(
                new Stay(room, request.checkIn(), request.checkOut(), request.companyCode()));
        Reservation draft = Reservation.pending(room, request.guestName(), request.guestEmail(),
                request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(),
                request.ratePlan(), quote);
        return database.inTransaction(c -> {
            reservations.lockRoom(c, room.number());
            if (reservations.isRoomBooked(c, room.number(), request.checkIn(), request.checkOut())) {
                throw new BusinessException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
            }
            long id = reservations.insert(c, draft);
            return reservations.findById(c, id, false).orElseThrow();
        });
    }

    public Reservation get(long id) {
        return database.inTransaction(c -> find(c, id, false));
    }

    public Reservation confirm(long id) {
        return change(id, Reservation::confirm, (c, r) -> messages.add(c, ConfirmationMessage.of(r)));
    }

    public Reservation cancel(long id) {
        return change(id, Reservation::cancel, (c, r) -> { });
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::checkIn, (c, r) -> { });
    }

    public Receipt checkOut(long id) {
        return change(id, Reservation::checkOut, (c, r) -> { }).receipt();
    }

    public Receipt receipt(long id) {
        return get(id).receipt();
    }

    public List<Message> sentMessages() {
        return database.inTransaction(messages::findAll);
    }

    private interface SideEffect {
        void run(Connection c, Reservation r) throws SQLException;
    }

    private Reservation change(long id, Consumer<Reservation> transition, SideEffect sideEffect) {
        return database.inTransaction(c -> {
            Reservation r = find(c, id, true);
            transition.accept(r);
            reservations.save(c, r);
            sideEffect.run(c, r);
            return r;
        });
    }

    private Reservation find(Connection c, long id, boolean forUpdate) throws SQLException {
        return reservations.findById(c, id, forUpdate)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND,
                        "Reserva não encontrada: " + id));
    }
}
