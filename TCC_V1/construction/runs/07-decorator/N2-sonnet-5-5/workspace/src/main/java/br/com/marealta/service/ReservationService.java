package br.com.marealta.service;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.NewReservation;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomCatalog;
import br.com.marealta.messaging.Message;
import br.com.marealta.messaging.MessageSender;
import br.com.marealta.persistence.ReservationRepository;

public final class ReservationService {
    private static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final ReservationRepository reservations;
    private final MessageSender sender;

    public ReservationService(ReservationRepository reservations, MessageSender sender) {
        this.reservations = reservations;
        this.sender = sender;
    }

    public synchronized Reservation create(NewReservation request) {
        Room room = RoomCatalog.find(request.roomNumber())
                .orElseThrow(() -> ApiException.roomNotFound(request.roomNumber()));
        if (request.guests() > room.capacity()) {
            throw ApiException.capacityExceeded(room.capacity());
        }
        Reservation reservation = Reservation.pending(room, request);
        if (!reservations.insertIfAvailable(reservation)) {
            throw ApiException.roomUnavailable();
        }
        return reservation;
    }

    public Reservation get(long id) {
        return reservations.find(id).orElseThrow(ApiException::reservationNotFound);
    }

    public synchronized Reservation confirm(long id) {
        Reservation reservation = get(id);
        reservation.confirm();
        reservations.update(reservation);
        sender.send(confirmationMessage(reservation));
        return reservation;
    }

    public synchronized Reservation cancel(long id) {
        Reservation reservation = get(id);
        reservation.cancel();
        reservations.update(reservation);
        return reservation;
    }

    public synchronized Reservation checkIn(long id) {
        Reservation reservation = get(id);
        reservation.registerCheckIn();
        reservations.update(reservation);
        return reservation;
    }

    public synchronized Receipt checkOut(long id) {
        Reservation reservation = get(id);
        reservation.registerCheckOut();
        reservations.update(reservation);
        return receiptOf(reservation);
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(long id) {
        Reservation reservation = get(id);
        reservation.requirePaid();
        return receiptOf(reservation);
    }

    private static Receipt receiptOf(Reservation reservation) {
        return Receipt.of(reservation, RoomCatalog.find(reservation.roomNumber()).orElseThrow());
    }

    private static Message confirmationMessage(Reservation reservation) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receiptOf(reservation).toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return Message.email(reservation.guestEmail(), CONFIRMATION_SUBJECT, body);
    }
}
