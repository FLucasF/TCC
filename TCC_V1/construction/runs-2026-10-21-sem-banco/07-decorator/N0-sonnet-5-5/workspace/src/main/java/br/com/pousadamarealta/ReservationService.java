package br.com.pousadamarealta;

import br.com.pousadamarealta.Reservation.Status;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    public record NewReservation(Integer roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    private final List<Reservation> reservations = new ArrayList<>();
    private final MessageDispatcher dispatcher;

    public ReservationService(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public synchronized Reservation create(NewReservation req) {
        if (!req.checkOut().isAfter(req.checkIn())) {
            throw ApiException.invalid("checkOut must be after checkIn");
        }
        if (req.guests() < 1) {
            throw ApiException.invalid("guests must be at least 1");
        }
        Room room = Room.find(req.roomNumber())
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Room not found: " + req.roomNumber()));
        if (req.guests() > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "Room " + room.number() + " holds at most " + room.capacity() + " guests");
        }
        boolean taken = reservations.stream().anyMatch(r -> r.room.equals(room) && r.active()
                && r.overlaps(req.checkIn(), req.checkOut()));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Room " + room.number() + " is not available");
        }
        Reservation r = new Reservation(reservations.size() + 1L, room, req.guestName(), req.guestEmail(),
                req.guestCpf(), req.checkIn(), req.checkOut(), req.guests(), req.ratePlan());
        reservations.add(r);
        return r;
    }

    public synchronized Reservation get(long id) {
        if (id < 1 || id > reservations.size()) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found: " + id);
        }
        return reservations.get((int) id - 1);
    }

    public synchronized Reservation confirm(long id) {
        Reservation r = require(id, Status.PENDING, "Only pending reservations can be confirmed");
        r.status = Status.CONFIRMED;
        r.amountPaid = r.total;
        dispatcher.sendEmail(r.guestEmail, "Reserva confirmada - Pousada Maré Alta", confirmationBody(r));
        return r;
    }

    public synchronized Reservation cancel(long id) {
        Reservation r = require(id, "Only pending or confirmed reservations can be cancelled",
                Status.PENDING, Status.CONFIRMED);
        r.refund = r.ratePlan.refundable() ? r.amountPaid : Money.ZERO;
        r.status = Status.CANCELLED;
        return r;
    }

    public synchronized Reservation checkIn(long id) {
        Reservation r = require(id, Status.CONFIRMED, "Only confirmed reservations can check in");
        r.status = Status.CHECKED_IN;
        return r;
    }

    public synchronized Reservation checkOut(long id) {
        Reservation r = require(id, Status.CHECKED_IN, "Only checked-in reservations can check out");
        r.status = Status.CHECKED_OUT;
        return r;
    }

    /** Receipt (second copy); only for paid reservations. */
    public synchronized Reservation forDocument(long id) {
        Reservation r = get(id);
        if (!r.paid()) {
            throw ApiException.notAllowed("Receipt is only available for paid reservations");
        }
        return r;
    }

    public synchronized List<Reservation> snapshot() {
        return List.copyOf(reservations);
    }

    private Reservation require(long id, Status expected, String message) {
        return require(id, message, expected);
    }

    private Reservation require(long id, String message, Status... allowed) {
        Reservation r = get(id);
        for (Status s : allowed) {
            if (r.status == s) return r;
        }
        throw ApiException.notAllowed(message);
    }

    static String confirmationBody(Reservation r) {
        return "Olá, " + r.guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.text(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
