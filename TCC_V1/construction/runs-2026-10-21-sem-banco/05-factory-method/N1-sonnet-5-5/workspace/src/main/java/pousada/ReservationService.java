package pousada;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import pousada.identity.DocumentData;
import pousada.identity.GuestIdentity;

public class ReservationService {
    private static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final List<Reservation> reservations = new ArrayList<>();
    private final Outbox outbox;

    public ReservationService(Outbox outbox) {
        this.outbox = outbox;
    }

    public synchronized Reservation create(int roomNumber, String guestName, String guestEmail, GuestIdentity identity,
                                           LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = RoomCatalog.find(roomNumber)
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + roomNumber + " não existe."));
        if (guests > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + roomNumber + " comporta no máximo " + room.capacity() + " pessoas.");
        }
        boolean taken = reservations.stream()
                .anyMatch(r -> r.room().equals(room)
                        && r.status() != ReservationStatus.CANCELLED
                        && r.overlaps(checkIn, checkOut));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "O quarto " + roomNumber + " já está reservado nesse período.");
        }
        Reservation reservation = new Reservation(reservations.size() + 1, room, guestName, guestEmail, identity,
                checkIn, checkOut, guests, ratePlan);
        reservations.add(reservation);
        return reservation;
    }

    public synchronized Reservation get(int id) {
        if (id < 1 || id > reservations.size()) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe.");
        }
        return reservations.get(id - 1);
    }

    public synchronized List<Reservation> all() {
        return List.copyOf(reservations);
    }

    public synchronized Reservation confirm(int id) {
        Reservation reservation = get(id);
        reservation.confirm();
        outbox.sendEmail(reservation.guestEmail(), CONFIRMATION_SUBJECT, confirmationBody(reservation));
        return reservation;
    }

    public synchronized Reservation cancel(int id) {
        Reservation reservation = get(id);
        reservation.cancel();
        return reservation;
    }

    public synchronized Reservation checkIn(int id) {
        Reservation reservation = get(id);
        reservation.startStay();
        return reservation;
    }

    public synchronized Reservation checkOut(int id) {
        Reservation reservation = get(id);
        reservation.endStay();
        return reservation;
    }

    /** Segunda via: só existe para reserva paga. */
    public synchronized Reservation paidReservation(int id) {
        Reservation reservation = get(id);
        if (!reservation.status().isPaid()) {
            throw ApiException.notAllowed("A reserva " + id + " está " + reservation.status() + " e não tem documento.");
        }
        return reservation;
    }

    public static String documentText(Reservation r) {
        return r.identity().renderDocument(new DocumentData(
                r.id(), r.guestName(), r.room(), r.checkIn(), r.checkOut(), r.quote()));
    }

    private static String confirmationBody(Reservation r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + documentText(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
