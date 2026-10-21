package br.com.pousadamarealta.reservas.service;

import br.com.pousadamarealta.reservas.domain.DomainException;
import br.com.pousadamarealta.reservas.domain.ErrorCode;
import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.Room;
import br.com.pousadamarealta.reservas.domain.RoomCatalog;
import br.com.pousadamarealta.reservas.messaging.Channel;
import br.com.pousadamarealta.reservas.messaging.Outbox;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Casos de uso das reservas. Os dados ficam só na memória. */
public final class ReservationService {

    private final RoomCatalog catalog;
    private final Outbox outbox;
    private final List<Reservation> reservations = new ArrayList<>();

    public ReservationService(RoomCatalog catalog, Outbox outbox) {
        this.catalog = catalog;
        this.outbox = outbox;
    }

    public synchronized Reservation create(NewReservation request) {
        if (request.guests() < 1) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "é preciso ao menos um hóspede");
        }
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "a saída deve ser depois da entrada");
        }
        Room room = catalog.find(request.roomNumber()).orElseThrow(() ->
                new DomainException(ErrorCode.ROOM_NOT_FOUND, "quarto " + request.roomNumber() + " não existe"));
        if (request.guests() > room.capacity()) {
            throw new DomainException(ErrorCode.CAPACITY_EXCEEDED,
                    "o quarto " + room.number() + " comporta até " + room.capacity() + " pessoas");
        }
        boolean taken = reservations.stream().anyMatch(other -> other.isActive()
                && other.room().number() == room.number()
                && other.overlaps(request.checkIn(), request.checkOut()));
        if (taken) {
            throw new DomainException(ErrorCode.ROOM_UNAVAILABLE,
                    "o quarto " + room.number() + " já está reservado em alguma das noites");
        }
        Reservation reservation = new Reservation(reservations.size() + 1L, room, request.guestName(),
                request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(),
                request.guests(), request.ratePlan());
        reservations.add(reservation);
        return reservation;
    }

    public synchronized Reservation get(long id) {
        return reservations.stream().filter(r -> r.id() == id).findFirst().orElseThrow(() ->
                new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "reserva " + id + " não existe"));
    }

    public synchronized List<Reservation> all() {
        return List.copyOf(reservations);
    }

    public synchronized Reservation confirm(long id) {
        return change(id, reservation -> {
            reservation.confirm();
            Receipt receipt = Receipt.of(reservation);
            outbox.send(Channel.EMAIL, reservation.guestEmail(), ConfirmationEmail.SUBJECT,
                    ConfirmationEmail.body(reservation.guestName(), receipt));
        });
    }

    public synchronized Reservation cancel(long id) {
        return change(id, Reservation::cancel);
    }

    public synchronized Reservation checkIn(long id) {
        return change(id, Reservation::registerCheckIn);
    }

    public synchronized Receipt checkOut(long id) {
        return Receipt.of(change(id, Reservation::registerCheckOut));
    }

    public synchronized Receipt document(long id) {
        Reservation reservation = get(id);
        reservation.requirePaid();
        return Receipt.of(reservation);
    }

    private Reservation change(long id, Consumer<Reservation> action) {
        Reservation reservation = get(id);
        action.accept(reservation);
        return reservation;
    }
}
