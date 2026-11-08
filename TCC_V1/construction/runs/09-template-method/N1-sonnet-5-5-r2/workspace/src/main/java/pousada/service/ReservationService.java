package pousada.service;

import pousada.domain.ApiException;
import pousada.domain.ErrorCode;
import pousada.domain.Message;
import pousada.domain.NewReservation;
import pousada.domain.Receipt;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.domain.RoomCatalog;
import pousada.persistence.Database;
import pousada.persistence.Tx;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ReservationService {
    private final Database database;
    private final RoomCatalog rooms;

    public ReservationService(Database database, RoomCatalog rooms) {
        this.database = database;
        this.rooms = rooms;
    }

    public List<Room> rooms() {
        return rooms.all();
    }

    public Reservation create(NewReservation request) {
        Room room = rooms.find(request.roomNumber()).orElseThrow(() ->
                new ApiException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + request.roomNumber() + " não existe"));
        if (request.guests() > room.capacity()) {
            throw new ApiException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        return database.inTransaction(tx -> {
            tx.lockRoom(room.number());
            if (tx.reservations().hasConflict(room.number(), request.checkIn(), request.checkOut())) {
                throw new ApiException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma dessas noites");
            }
            return tx.reservations().insert(Reservation.open(room, request));
        });
    }

    public Reservation get(long id) {
        return database.inTransaction(tx -> tx.reservations().find(id).orElseThrow(() -> notFound(id)));
    }

    public Reservation confirm(long id) {
        return change(id, Reservation::confirm, (tx, r) -> {
            tx.messages().save(ConfirmationEmail.build(r, receiptOf(r)));
            return r;
        });
    }

    public Reservation cancel(long id) {
        return change(id, Reservation::cancel, (tx, r) -> r);
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::registerCheckIn, (tx, r) -> r);
    }

    public Receipt checkOut(long id) {
        return change(id, Reservation::registerCheckOut, (tx, r) -> receiptOf(r));
    }

    /** Segunda via do comprovante, só para reserva paga. */
    public Receipt document(long id) {
        Reservation reservation = get(id);
        reservation.requirePaid();
        return receiptOf(reservation);
    }

    public List<Message> messages() {
        return database.inTransaction(tx -> tx.messages().all());
    }

    /** Trava a reserva, aplica a mudança de situação, grava e deixa o caso completar o que é dele. */
    private <R> R change(long id, Consumer<Reservation> action, BiFunction<Tx, Reservation, R> then) {
        return database.inTransaction(tx -> {
            Reservation reservation = tx.reservations().findForUpdate(id).orElseThrow(() -> notFound(id));
            action.accept(reservation);
            tx.reservations().update(reservation);
            return then.apply(tx, reservation);
        });
    }

    private Receipt receiptOf(Reservation reservation) {
        return Receipt.of(reservation, rooms.get(reservation.roomNumber()));
    }

    private static ApiException notFound(long id) {
        return new ApiException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }
}
