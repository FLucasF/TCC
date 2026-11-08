package com.marealta;

public final class ReservationService {
    private final RoomCatalog rooms;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    public ReservationService(RoomCatalog rooms, ReservationRepository reservations, MessageRepository messages) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
    }

    public synchronized Reservation create(ReservationRequest request) {
        Room room = rooms.find(request.roomNumber()).orElseThrow(() -> ApiException.roomNotFound(request.roomNumber()));
        if (request.guests() > room.capacity()) {
            throw ApiException.capacityExceeded(room.capacity());
        }
        if (reservations.existsOverlap(room.number(), request.checkIn(), request.checkOut())) {
            throw ApiException.roomUnavailable(room.number());
        }
        Quote quote = Quote.of(room, request.checkIn(), request.checkOut(), request.ratePlan());
        Reservation reservation = Reservation.pending(request, quote);
        reservations.insert(reservation);
        return reservation;
    }

    public Reservation get(String id) {
        try {
            return reservations.find(Long.parseLong(id)).orElseThrow(() -> ApiException.reservationNotFound(id));
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(id);
        }
    }

    public synchronized Reservation confirm(String id) {
        Reservation reservation = get(id);
        reservation.confirm();
        reservations.update(reservation);
        messages.save(confirmationEmail(reservation));
        return reservation;
    }

    public synchronized Reservation cancel(String id) {
        Reservation reservation = get(id);
        reservation.cancel();
        reservations.update(reservation);
        return reservation;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation reservation = get(id);
        reservation.startStay();
        reservations.update(reservation);
        return reservation;
    }

    public synchronized Receipt checkOut(String id) {
        Reservation reservation = get(id);
        reservation.endStay();
        reservations.update(reservation);
        return receipt(reservation);
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(String id) {
        Reservation reservation = get(id);
        if (!reservation.status().isPaid()) {
            throw ApiException.operationNotAllowed("Só existe comprovante para reserva paga");
        }
        return receipt(reservation);
    }

    private Receipt receipt(Reservation reservation) {
        return Receipt.of(reservation, rooms.find(reservation.roomNumber()).orElseThrow());
    }

    private Message confirmationEmail(Reservation reservation) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt(reservation).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(null, Message.Channel.EMAIL, reservation.guestEmail(),
                "Reserva confirmada - Pousada Maré Alta", body);
    }
}
