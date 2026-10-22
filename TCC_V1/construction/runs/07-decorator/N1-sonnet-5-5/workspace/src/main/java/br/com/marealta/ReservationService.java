package br.com.marealta;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.NewReservation;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.mail.ConfirmationEmail;
import br.com.marealta.mail.Mailer;
import br.com.marealta.persistence.Db;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.persistence.RoomRepository;

import java.sql.Connection;
import java.sql.SQLException;

public final class ReservationService {
    private final Db db;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final Mailer mailer;

    public ReservationService(Db db, RoomRepository rooms, ReservationRepository reservations, Mailer mailer) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
        this.mailer = mailer;
    }

    public Reservation create(NewReservation request) {
        return db.transaction(connection -> {
            Room room = rooms.findForUpdate(connection, request.roomNumber())
                    .orElseThrow(() -> new DomainException(ErrorCode.ROOM_NOT_FOUND, "Quarto não encontrado"));
            if (request.guests() > room.capacity()) {
                throw new DomainException(ErrorCode.CAPACITY_EXCEEDED,
                        "O quarto " + room.number() + " comporta até " + room.capacity() + " pessoas");
            }
            if (reservations.existsOverlapping(connection, room.number(), request.checkIn(), request.checkOut())) {
                throw new DomainException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites");
            }
            Reservation reservation = Reservation.pending(room, request.guest(), request.checkIn(),
                    request.checkOut(), request.guests(), request.ratePlan());
            long id = reservations.insert(connection, reservation);
            return reservations.find(connection, id).orElseThrow();
        });
    }

    public Reservation get(long id) {
        return db.transaction(connection -> load(connection, id, false));
    }

    public Reservation confirm(long id) {
        return db.transaction(connection -> {
            Reservation reservation = load(connection, id, true);
            reservation.confirm();
            reservations.updateState(connection, reservation);
            Receipt receipt = receiptOf(connection, reservation);
            mailer.send(connection, ConfirmationEmail.to(reservation.guest().email(),
                    reservation.guest().name(), receipt));
            return reservation;
        });
    }

    public Reservation cancel(long id) {
        return change(id, Reservation::cancel);
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::checkIn);
    }

    public Receipt checkOut(long id) {
        return db.transaction(connection -> {
            Reservation reservation = load(connection, id, true);
            reservation.checkOut();
            reservations.updateState(connection, reservation);
            return receiptOf(connection, reservation);
        });
    }

    /** A segunda via do comprovante, que só existe para reserva paga. */
    public Receipt document(long id) {
        return db.transaction(connection -> {
            Reservation reservation = load(connection, id, false);
            ReservationStatus status = reservation.status();
            if (!status.isPaid()) {
                throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "Só há comprovante para reserva paga");
            }
            return receiptOf(connection, reservation);
        });
    }

    private Reservation change(long id, java.util.function.Consumer<Reservation> operation) {
        return db.transaction(connection -> {
            Reservation reservation = load(connection, id, true);
            operation.accept(reservation);
            reservations.updateState(connection, reservation);
            return reservation;
        });
    }

    private Reservation load(Connection connection, long id, boolean forUpdate) throws SQLException {
        return (forUpdate ? reservations.findForUpdate(connection, id) : reservations.find(connection, id))
                .orElseThrow(() -> new DomainException(ErrorCode.RESERVATION_NOT_FOUND,
                        "Reserva não encontrada"));
    }

    private Receipt receiptOf(Connection connection, Reservation reservation) throws SQLException {
        Room room = rooms.find(connection, reservation.roomNumber()).orElseThrow();
        return Receipt.of(reservation, room);
    }
}
