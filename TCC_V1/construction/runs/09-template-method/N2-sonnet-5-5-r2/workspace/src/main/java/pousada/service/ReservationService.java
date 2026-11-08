package pousada.service;

import pousada.domain.AppException;
import pousada.domain.Receipt;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.domain.RoomCatalog;
import pousada.repo.ReservationRepository;

public class ReservationService {
    private final ReservationRepository reservations;
    private final ConfirmationMailer mailer;

    public ReservationService(ReservationRepository reservations, ConfirmationMailer mailer) {
        this.reservations = reservations;
        this.mailer = mailer;
    }

    public synchronized Reservation create(NewReservation req) {
        Room room = RoomCatalog.require(req.roomNumber());
        if (req.guests() > room.capacity()) {
            throw new AppException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        if (reservations.hasOverlap(room.number(), req.checkIn(), req.checkOut())) {
            throw new AppException(409, "ROOM_UNAVAILABLE", "O quarto já está reservado em alguma dessas noites");
        }
        return reservations.insert(Reservation.pending(room, req.guestName(), req.guestEmail(), req.guestCpf(),
                req.checkIn(), req.checkOut(), req.guests(), req.ratePlan()));
    }

    public Reservation get(long id) {
        return reservations.find(id).orElseThrow(() ->
                new AppException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe"));
    }

    public synchronized Reservation confirm(long id) {
        Reservation r = get(id);
        r.confirm();
        reservations.update(r);
        mailer.send(r);
        return r;
    }

    public synchronized Reservation checkIn(long id) {
        Reservation r = get(id);
        r.doCheckIn();
        reservations.update(r);
        return r;
    }

    public synchronized Receipt checkOut(long id) {
        Reservation r = get(id);
        r.doCheckOut();
        reservations.update(r);
        return Receipt.of(r);
    }

    public synchronized Reservation cancel(long id) {
        Reservation r = get(id);
        r.cancel();
        reservations.update(r);
        return r;
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt receipt(long id) {
        Reservation r = get(id);
        if (!r.status().isPaid()) {
            throw AppException.notAllowed("Só uma reserva paga tem comprovante");
        }
        return Receipt.of(r);
    }
}
