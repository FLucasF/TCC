package br.com.marealta.service;

import br.com.marealta.domain.AppException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.identity.DocumentData;
import br.com.marealta.identity.GuestIdentity;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.MessageRepository;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.persistence.RoomRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, GuestIdentity identity,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    /** A reserva junto com os dados do quarto, o suficiente para montar o documento. */
    public record Stay(Reservation reservation, Room room) {
        public DocumentData document() {
            Reservation r = reservation;
            return new DocumentData(r.id(), r.guestName(), r.identity(), room.number(), room.type(),
                    r.checkIn(), r.checkOut(), r.quote());
        }
    }

    private final Database database;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    public ReservationService(Database database, RoomRepository rooms, ReservationRepository reservations,
                              MessageRepository messages) {
        this.database = database;
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
    }

    public List<Room> listRooms() {
        return database.inTransaction(rooms::findAll);
    }

    public List<MessageRepository.Message> listMessages() {
        return database.inTransaction(messages::findAll);
    }

    public Reservation create(NewReservation request) {
        return database.inTransaction(connection -> {
            Room room = rooms.findForUpdate(connection, request.roomNumber()).orElseThrow(
                    () -> new AppException(404, "ROOM_NOT_FOUND", "Quarto " + request.roomNumber() + " não existe"));
            if (request.guests() > room.capacity()) {
                throw new AppException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta até " + room.capacity() + " hóspedes");
            }
            if (reservations.existsOverlap(connection, room.number(), request.checkIn(), request.checkOut())) {
                throw new AppException(409, "ROOM_UNAVAILABLE",
                        "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
            }
            Quote quote = Quote.of(room, request.checkIn(), request.checkOut(), request.ratePlan());
            Reservation draft = new Reservation(0, room.number(), request.guestName(), request.guestEmail(),
                    request.identity(), request.checkIn(), request.checkOut(), request.guests(), request.ratePlan(),
                    quote, ReservationStatus.PENDING, Money.ZERO, null);
            long id = reservations.insert(connection, draft);
            return reservations.find(connection, id).orElseThrow();
        });
    }

    public Reservation get(long id) {
        return database.inTransaction(connection -> load(connection, id, false));
    }

    public Reservation confirm(long id) {
        return database.inTransaction(connection -> {
            Reservation reservation = load(connection, id, true);
            reservation.confirm();
            reservations.update(connection, reservation);
            ConfirmationEmail email = ConfirmationEmail.of(new Stay(reservation, room(connection, reservation)));
            messages.save(connection, "EMAIL", reservation.guestEmail(), email.subject(), email.body());
            return reservation;
        });
    }

    public Reservation cancel(long id) {
        return transition(id, Reservation::cancel);
    }

    public Reservation checkIn(long id) {
        return transition(id, Reservation::startStay);
    }

    public Stay checkOut(long id) {
        return database.inTransaction(connection -> {
            Reservation reservation = load(connection, id, true);
            reservation.endStay();
            reservations.update(connection, reservation);
            return new Stay(reservation, room(connection, reservation));
        });
    }

    /** Segunda via do documento, só para reserva paga. */
    public Stay duplicate(long id) {
        return database.inTransaction(connection -> {
            Reservation reservation = load(connection, id, false);
            reservation.requirePaid();
            return new Stay(reservation, room(connection, reservation));
        });
    }

    private Reservation transition(long id, java.util.function.Consumer<Reservation> change) {
        return database.inTransaction(connection -> {
            Reservation reservation = load(connection, id, true);
            change.accept(reservation);
            reservations.update(connection, reservation);
            return reservation;
        });
    }

    private Reservation load(Connection connection, long id, boolean lock) throws SQLException {
        return (lock ? reservations.findForUpdate(connection, id) : reservations.find(connection, id)).orElseThrow(
                () -> new AppException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe"));
    }

    private Room room(Connection connection, Reservation reservation) throws SQLException {
        return rooms.find(connection, reservation.roomNumber()).orElseThrow();
    }
}
