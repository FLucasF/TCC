package br.com.pousadamarealta.reservas.service;

import br.com.pousadamarealta.reservas.domain.DomainException;
import br.com.pousadamarealta.reservas.domain.ErrorCode;
import br.com.pousadamarealta.reservas.domain.Receipt;
import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.Room;
import br.com.pousadamarealta.reservas.notification.Message;
import br.com.pousadamarealta.reservas.notification.Notifier;
import br.com.pousadamarealta.reservas.notification.ReservationEvent;
import br.com.pousadamarealta.reservas.persistence.Database;
import br.com.pousadamarealta.reservas.persistence.MessageRepository;
import br.com.pousadamarealta.reservas.persistence.ReservationRepository;
import br.com.pousadamarealta.reservas.persistence.RoomRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Casos de uso das reservas. Cada operação é uma transação, inclusive as mensagens que ela dispara. */
public class ReservationService {
    private final Database database;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MessageRepository messages;
    private final List<Notifier> notifiers;

    public ReservationService(Database database, RoomRepository rooms, ReservationRepository reservations,
                              MessageRepository messages, List<Notifier> notifiers) {
        this.database = database;
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
        this.notifiers = List.copyOf(notifiers);
    }

    public List<Room> listRooms() {
        return database.inTransaction(rooms::findAll);
    }

    public List<Message> listMessages() {
        return database.inTransaction(messages::findAll);
    }

    public Reservation create(NewReservation request) {
        return database.inTransaction(c -> {
            Room room = rooms.findForUpdate(c, request.roomNumber()).orElseThrow(
                    () -> new DomainException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + request.roomNumber() + " não existe"));
            Reservation reservation = Reservation.create(room, request.guestName(), request.guestEmail(),
                    request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(), request.ratePlan());
            if (reservations.hasOverlap(c, room.number(), request.checkIn(), request.checkOut())) {
                throw new DomainException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
            }
            return reservations.insert(c, reservation);
        });
    }

    public Reservation get(long id) {
        return database.inTransaction(c -> load(c, id, false));
    }

    public Reservation confirm(long id) {
        return database.inTransaction(c -> {
            Reservation confirmed = load(c, id, true).confirm();
            reservations.updateState(c, confirmed);
            publish(c, new ReservationEvent.Confirmed(confirmed, room(c, confirmed)));
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return database.inTransaction(c -> {
            Reservation before = load(c, id, true);
            Reservation cancelled = before.cancel();
            reservations.updateState(c, cancelled);
            boolean wasConfirmed = before.status().isPaid();
            publish(c, new ReservationEvent.Cancelled(cancelled, room(c, cancelled), wasConfirmed));
            return cancelled;
        });
    }

    public Reservation checkIn(long id) {
        return database.inTransaction(c -> {
            Reservation checkedIn = load(c, id, true).doCheckIn();
            reservations.updateState(c, checkedIn);
            return checkedIn;
        });
    }

    public Receipt checkOut(long id) {
        return database.inTransaction(c -> {
            Reservation checkedOut = load(c, id, true).doCheckOut();
            reservations.updateState(c, checkedOut);
            return Receipt.of(checkedOut, room(c, checkedOut));
        });
    }

    /** Segunda via do comprovante: só existe para reserva paga. */
    public Receipt receipt(long id) {
        return database.inTransaction(c -> {
            Reservation reservation = load(c, id, false);
            if (!reservation.status().isPaid()) {
                throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "Só há comprovante para reserva paga; status atual: " + reservation.status());
            }
            return Receipt.of(reservation, room(c, reservation));
        });
    }

    private Reservation load(Connection c, long id, boolean forUpdate) throws SQLException {
        var found = forUpdate ? reservations.findForUpdate(c, id) : reservations.find(c, id);
        return found.orElseThrow(
                () -> new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe"));
    }

    private Room room(Connection c, Reservation reservation) throws SQLException {
        return rooms.find(c, reservation.roomNumber()).orElseThrow(
                () -> new IllegalStateException("Quarto " + reservation.roomNumber() + " desapareceu"));
    }

    private void publish(Connection c, ReservationEvent event) throws SQLException {
        for (Notifier notifier : notifiers) {
            for (Message message : notifier.messagesFor(event)) {
                messages.insert(c, message);
            }
        }
    }
}
