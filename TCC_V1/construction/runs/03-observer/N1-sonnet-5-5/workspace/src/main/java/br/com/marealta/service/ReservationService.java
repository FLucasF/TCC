package br.com.marealta.service;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.notification.ReservationEvent;
import br.com.marealta.notification.ReservationEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ReservationService {
    private final RoomCatalog rooms;
    private final ReservationEvents events;
    private final List<Reservation> reservations = new ArrayList<>();

    public ReservationService(RoomCatalog rooms, ReservationEvents events) {
        this.rooms = rooms;
        this.events = events;
    }

    public synchronized Reservation create(NewReservation request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "A saída deve ser depois da entrada");
        }
        if (request.guests() < 1) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "É preciso pelo menos um hóspede");
        }
        Room room = rooms.get(request.roomNumber());
        if (request.guests() > room.capacity()) {
            throw new DomainException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto %d comporta no máximo %d hóspedes".formatted(room.number(), room.capacity()));
        }
        boolean taken = reservations.stream()
                .filter(r -> r.room().equals(room) && r.status() != ReservationStatus.CANCELLED)
                .anyMatch(r -> r.overlaps(request.checkIn(), request.checkOut()));
        if (taken) {
            throw new DomainException(ErrorCode.ROOM_UNAVAILABLE,
                    "O quarto %d já está reservado em alguma das noites".formatted(room.number()));
        }
        Reservation reservation = new Reservation(reservations.size() + 1L, room, request.guestName(),
                request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(),
                request.guests(), request.ratePlan());
        reservations.add(reservation);
        return reservation;
    }

    public synchronized Reservation get(long id) {
        if (id < 1 || id > reservations.size()) {
            throw new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não existe: " + id);
        }
        return reservations.get((int) id - 1);
    }

    public synchronized Reservation confirm(long id) {
        Reservation reservation = get(id);
        reservation.confirm();
        events.publish(new ReservationEvent.Confirmed(reservation));
        return reservation;
    }

    public synchronized Reservation cancel(long id) {
        Reservation reservation = get(id);
        ReservationStatus previous = reservation.status();
        reservation.cancel();
        events.publish(new ReservationEvent.Cancelled(reservation, previous));
        return reservation;
    }

    public synchronized Reservation checkIn(long id) {
        Reservation reservation = get(id);
        reservation.checkIn();
        return reservation;
    }

    public synchronized Receipt checkOut(long id) {
        Reservation reservation = get(id);
        reservation.checkOut();
        return reservation.receipt();
    }

    public synchronized Receipt receipt(long id) {
        return get(id).receipt();
    }

    /** Roda uma consulta (relatório) sobre as reservas sem que elas mudem no meio. */
    public synchronized <T> T query(Function<List<Reservation>, T> query) {
        return query.apply(List.copyOf(reservations));
    }
}
