package com.marealta.service;

import com.marealta.domain.ErrorCode;
import com.marealta.domain.HotelException;
import com.marealta.domain.Receipt;
import com.marealta.domain.Reservation;
import com.marealta.domain.Room;
import com.marealta.mail.Message;
import com.marealta.mail.Outbox;
import com.marealta.persistence.Database;
import com.marealta.persistence.ReservationRepository;
import com.marealta.persistence.RoomRepository;

import java.util.List;
import java.util.function.Consumer;

public final class ReservationService {
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

    public List<Room> rooms() {
        return database.transaction(rooms::all);
    }

    public Reservation create(NewReservation request) {
        return database.transaction(c -> {
            Room room = rooms.findForUpdate(c, request.roomNumber()).orElseThrow(() ->
                    new HotelException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + request.roomNumber() + " não existe"));
            Reservation reservation = Reservation.open(room, request.guestName(), request.guestEmail(),
                    request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(),
                    request.ratePlan());
            if (reservations.isRoomTaken(c, room.number(), request.checkIn(), request.checkOut())) {
                throw new HotelException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites");
            }
            reservations.insert(c, reservation);
            return reservation;
        });
    }

    public Reservation get(String id) {
        long parsed = parseId(id);
        return database.transaction(c -> reservations.find(c, parsed, false).orElseThrow(() -> notFound(id)));
    }

    public Reservation confirm(String id) {
        Reservation reservation = change(id, Reservation::confirm);
        outbox.send(Message.email(reservation.guestEmail(), ConfirmationEmail.SUBJECT,
                ConfirmationEmail.body(reservation.guestName(), reservation.receipt())));
        return reservation;
    }

    public Reservation cancel(String id) {
        return change(id, Reservation::cancel);
    }

    public Reservation checkIn(String id) {
        return change(id, Reservation::startStay);
    }

    public Receipt checkOut(String id) {
        return change(id, Reservation::endStay).receipt();
    }

    public Receipt document(String id) {
        return get(id).receipt();
    }

    private Reservation change(String id, Consumer<Reservation> transition) {
        long parsed = parseId(id);
        return database.transaction(c -> {
            Reservation reservation = reservations.find(c, parsed, true).orElseThrow(() -> notFound(id));
            transition.accept(reservation);
            reservations.update(c, reservation);
            return reservation;
        });
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw notFound(id);
        }
    }

    private static HotelException notFound(String id) {
        return new HotelException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }
}
