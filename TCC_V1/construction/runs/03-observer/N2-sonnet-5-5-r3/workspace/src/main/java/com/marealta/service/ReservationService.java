package com.marealta.service;

import com.marealta.domain.ErrorCode;
import com.marealta.domain.PousadaException;
import com.marealta.domain.Receipt;
import com.marealta.domain.Reservation;
import com.marealta.domain.ReservationStatus;
import com.marealta.domain.Room;
import com.marealta.domain.RoomCatalog;
import com.marealta.events.ReservationEvent;
import com.marealta.events.ReservationListener;
import com.marealta.persistence.Database;
import com.marealta.persistence.ReservationRepository;

import java.util.List;
import java.util.function.Consumer;

public final class ReservationService {
    private final Database db;
    private final ReservationRepository reservations;
    private final RoomCatalog rooms;
    private final List<ReservationListener> listeners;

    public ReservationService(Database db, ReservationRepository reservations, RoomCatalog rooms,
                              List<ReservationListener> listeners) {
        this.db = db;
        this.reservations = reservations;
        this.rooms = rooms;
        this.listeners = List.copyOf(listeners);
    }

    public Reservation create(NewReservation cmd) {
        return db.transaction(() -> {
            Room room = rooms.require(cmd.roomNumber());
            if (cmd.guests() > room.capacity()) {
                throw new PousadaException(ErrorCode.CAPACITY_EXCEEDED,
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
            }
            reservations.lockBookings();
            if (reservations.isRoomBooked(room.number(), cmd.checkIn(), cmd.checkOut())) {
                throw new PousadaException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
            }
            Reservation r = Reservation.open(room, cmd.guestName(), cmd.guestEmail(), cmd.guestCpf(),
                    cmd.checkIn(), cmd.checkOut(), cmd.guests(), cmd.ratePlan());
            reservations.insert(r);
            return r;
        });
    }

    public Reservation get(long id) {
        return db.transaction(() -> load(id, false));
    }

    public Reservation confirm(long id) {
        return change(id, Reservation::confirm, ReservationEvent.Type.CONFIRMED);
    }

    public Reservation cancel(long id) {
        return change(id, Reservation::cancel, ReservationEvent.Type.CANCELLED);
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::registerCheckIn, ReservationEvent.Type.CHECKED_IN);
    }

    public Receipt checkOut(long id) {
        Reservation r = change(id, Reservation::registerCheckOut, ReservationEvent.Type.CHECKED_OUT);
        return Receipt.of(r, rooms.require(r.roomNumber()));
    }

    /** Segunda via do comprovante: só existe para reserva paga. */
    public Receipt document(long id) {
        return db.transaction(() -> {
            Reservation r = load(id, false);
            if (!r.status().isPaid()) {
                throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "O comprovante só existe para reserva paga");
            }
            return Receipt.of(r, rooms.require(r.roomNumber()));
        });
    }

    /** A sequência é a mesma para toda mudança: trancar, mudar, gravar e avisar quem quer saber. */
    private Reservation change(long id, Consumer<Reservation> change, ReservationEvent.Type type) {
        return db.transaction(() -> {
            Reservation r = load(id, true);
            ReservationStatus previous = r.status();
            change.accept(r);
            reservations.update(r);
            ReservationEvent event = new ReservationEvent(type, r, rooms.require(r.roomNumber()), previous);
            listeners.forEach(l -> l.onEvent(event));
            return r;
        });
    }

    private Reservation load(long id, boolean lock) {
        return (lock ? reservations.findForUpdate(id) : reservations.find(id)).orElseThrow(() ->
                new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe"));
    }
}
