package br.com.marealta.reservas.service;

import br.com.marealta.reservas.domain.DomainException;
import br.com.marealta.reservas.domain.ErrorCode;
import br.com.marealta.reservas.domain.Quote;
import br.com.marealta.reservas.domain.Receipt;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.Room;
import br.com.marealta.reservas.notification.ReservationCancelled;
import br.com.marealta.reservas.notification.ReservationConfirmed;
import br.com.marealta.reservas.notification.ReservationListener;
import br.com.marealta.reservas.persistence.Database;
import br.com.marealta.reservas.persistence.ReservationRepository;
import br.com.marealta.reservas.persistence.RoomRepository;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ReservationService {
    private final Database db;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final List<ReservationListener> listeners;

    public ReservationService(Database db, RoomRepository rooms, ReservationRepository reservations,
                              List<ReservationListener> listeners) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
        this.listeners = List.copyOf(listeners);
    }

    public Reservation create(NewReservation request) {
        if (request.guests() < 1) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "É preciso ao menos um hóspede");
        }
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "A saída deve ser depois da entrada");
        }
        return db.inTransaction(() -> {
            Room room = rooms.findForUpdate(request.roomNumber()).orElseThrow(() ->
                    new DomainException(ErrorCode.ROOM_NOT_FOUND, "Quarto não encontrado: " + request.roomNumber()));
            if (request.guests() > room.capacity()) {
                throw new DomainException(ErrorCode.CAPACITY_EXCEEDED,
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
            }
            if (reservations.hasActiveOverlap(room.number(), request.checkIn(), request.checkOut())) {
                throw new DomainException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
            }
            Quote quote = Quote.of(room, request.checkIn(), request.checkOut(), request.ratePlan());
            Reservation reservation = Reservation.pending(room.number(), request.guestName(),
                    request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(),
                    request.guests(), request.ratePlan(), quote);
            reservations.insert(reservation);
            return reservation;
        });
    }

    public Reservation get(long id) {
        return db.inTransaction(() -> reservations.find(id).orElseThrow(() -> notFound(id)));
    }

    public Reservation confirm(long id) {
        return change(id, Reservation::confirm, (reservation, room) -> {
            ReservationConfirmed event = new ReservationConfirmed(reservation, room);
            listeners.forEach(listener -> listener.onConfirmed(event));
        });
    }

    public Reservation cancel(long id) {
        return change(id, Reservation::cancel, (reservation, room) -> {
            ReservationCancelled event = new ReservationCancelled(reservation, room);
            listeners.forEach(listener -> listener.onCancelled(event));
        });
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::registerCheckIn, (reservation, room) -> { });
    }

    public Receipt checkOut(long id) {
        return db.inTransaction(() -> {
            Reservation reservation = reservations.findForUpdate(id).orElseThrow(() -> notFound(id));
            reservation.registerCheckOut();
            reservations.update(reservation);
            return Receipt.of(reservation, roomOf(reservation));
        });
    }

    public Receipt document(long id) {
        return db.inTransaction(() -> {
            Reservation reservation = reservations.find(id).orElseThrow(() -> notFound(id));
            reservation.requireReceipt();
            return Receipt.of(reservation, roomOf(reservation));
        });
    }

    public List<Room> rooms() {
        return rooms.findAll();
    }

    private Reservation change(long id, Consumer<Reservation> transition,
                               BiConsumer<Reservation, Room> announcement) {
        return db.inTransaction(() -> {
            Reservation reservation = reservations.findForUpdate(id).orElseThrow(() -> notFound(id));
            transition.accept(reservation);
            reservations.update(reservation);
            announcement.accept(reservation, roomOf(reservation));
            return reservation;
        });
    }

    private Room roomOf(Reservation reservation) {
        return rooms.find(reservation.roomNumber()).orElseThrow();
    }

    private static DomainException notFound(long id) {
        return new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não encontrada: " + id);
    }
}
