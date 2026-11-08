package com.pousada.service;

import com.pousada.domain.DomainException;
import com.pousada.domain.ErrorCode;
import com.pousada.domain.Pricing;
import com.pousada.domain.Receipt;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.domain.RoomCatalog;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;

public class ReservationService {
    private final RoomCatalog rooms;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    public ReservationService(RoomCatalog rooms, ReservationRepository reservations, MessageRepository messages) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
    }

    public Reservation create(NewReservation request) {
        Room room = rooms.get(request.roomNumber());
        if (request.guests() > room.capacity()) {
            throw new DomainException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        Pricing.Quote quote = Pricing.quote(room, request.checkIn(), request.checkOut(), request.ratePlan());
        return reservations.create(Reservation.pending(0, room.number(), request.guestName(),
                request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(),
                request.guests(), request.ratePlan(), quote));
    }

    public Reservation get(long id) {
        return reservations.find(id).orElseThrow(() -> new DomainException(
                ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe"));
    }

    public Reservation confirm(long id) {
        Reservation reservation = reservations.update(id, Reservation::confirm);
        messages.save(ConfirmationEmail.of(reservation.getGuestName(), reservation.getGuestEmail(),
                receiptOf(reservation)));
        return reservation;
    }

    public Reservation cancel(long id) {
        return reservations.update(id, Reservation::cancel);
    }

    public Reservation checkIn(long id) {
        return reservations.update(id, Reservation::checkIn);
    }

    public Receipt checkOut(long id) {
        return receiptOf(reservations.update(id, Reservation::checkOut));
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(long id) {
        Reservation reservation = get(id);
        if (!reservation.getStatus().paid()) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Reserva " + id + " está " + reservation.getStatus() + " e não tem comprovante");
        }
        return receiptOf(reservation);
    }

    private Receipt receiptOf(Reservation reservation) {
        return Receipt.of(reservation, rooms.get(reservation.getRoomNumber()));
    }
}
