package com.marealta.pousada.domain;

import com.marealta.pousada.mail.MailService;
import com.marealta.pousada.persistence.ReservationRepository;

public final class ReservationService {
    private final ReservationRepository reservations;
    private final RoomCatalog rooms;
    private final MailService mail;

    public ReservationService(ReservationRepository reservations, RoomCatalog rooms, MailService mail) {
        this.reservations = reservations;
        this.rooms = rooms;
        this.mail = mail;
    }

    public synchronized Reservation create(NewReservation request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "A saída deve ser depois da entrada");
        }
        if (request.guests() < 1) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "A reserva precisa de pelo menos um hóspede");
        }
        Room room = room(request.roomNumber());
        if (request.guests() > room.capacity()) {
            throw new DomainException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta até " + room.capacity() + " pessoas");
        }
        if (!reservations.findActiveOverlapping(room.number(), request.checkIn(), request.checkOut()).isEmpty()) {
            throw new DomainException(ErrorCode.ROOM_UNAVAILABLE,
                    "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
        }
        Quote quote = Quote.of(room.type(), request.ratePlan(), request.checkIn(), request.checkOut());
        Reservation reservation = Reservation.open(request, quote);
        reservation.assignId(reservations.insert(reservation));
        return reservation;
    }

    public Reservation find(long id) {
        return reservations.find(id).orElseThrow(() ->
                new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não encontrada"));
    }

    public synchronized Reservation confirm(long id) {
        Reservation reservation = find(id);
        reservation.confirm();
        reservations.update(reservation);
        mail.sendEmail(reservation.guestEmail(), "Reserva confirmada - Pousada Maré Alta", confirmationText(reservation));
        return reservation;
    }

    public synchronized Reservation cancel(long id) {
        Reservation reservation = find(id);
        reservation.cancel();
        reservations.update(reservation);
        return reservation;
    }

    public synchronized Reservation checkIn(long id) {
        Reservation reservation = find(id);
        reservation.registerCheckIn();
        reservations.update(reservation);
        return reservation;
    }

    public synchronized ReservationDocument checkOut(long id) {
        Reservation reservation = find(id);
        reservation.registerCheckOut();
        reservations.update(reservation);
        return documentOf(reservation);
    }

    /** Segunda via do comprovante; só existe para reserva paga. */
    public ReservationDocument document(long id) {
        Reservation reservation = find(id);
        if (!reservation.status().isPaid()) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Não há comprovante para reserva com status " + reservation.status());
        }
        return documentOf(reservation);
    }

    private ReservationDocument documentOf(Reservation reservation) {
        return ReservationDocument.of(reservation, room(reservation.roomNumber()));
    }

    private Room room(int number) {
        return rooms.find(number).orElseThrow(() ->
                new DomainException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + number + " não existe"));
    }

    private String confirmationText(Reservation reservation) {
        return "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + documentOf(reservation).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
