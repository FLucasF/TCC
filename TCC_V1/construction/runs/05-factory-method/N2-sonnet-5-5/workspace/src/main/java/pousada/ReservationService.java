package pousada;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ReservationService {
    private final RoomCatalog rooms;
    private final Outbox outbox;
    private final TreeMap<Integer, Reservation> reservations = new TreeMap<>();

    public ReservationService(RoomCatalog rooms, Outbox outbox) {
        this.rooms = rooms;
        this.outbox = outbox;
    }

    public synchronized Reservation create(NewReservation request) {
        Room room = rooms.find(request.roomNumber())
                .orElseThrow(() -> ApiException.roomNotFound(request.roomNumber()));
        if (request.guests() > room.capacity()) {
            throw ApiException.capacityExceeded(room.capacity());
        }
        boolean taken = reservations.values().stream().anyMatch(r ->
                r.room().equals(room) && r.overlaps(request.checkIn(), request.checkOut()));
        if (taken) {
            throw ApiException.roomUnavailable(room.number());
        }
        Reservation reservation = new Reservation(reservations.size() + 1, room, request.guestName(),
                request.guestEmail(), request.identity(), request.checkIn(), request.checkOut(),
                request.guests(), request.ratePlan());
        reservations.put(reservation.id(), reservation);
        return reservation;
    }

    public synchronized Reservation find(String id) {
        try {
            Reservation reservation = reservations.get(Integer.parseInt(id));
            if (reservation != null) return reservation;
        } catch (NumberFormatException ignored) {
            // id que não é número não existe
        }
        throw ApiException.reservationNotFound(id);
    }

    public synchronized Reservation confirm(String id) {
        Reservation reservation = find(id);
        reservation.confirm();
        outbox.sendEmail(reservation.guestEmail(), ConfirmationEmail.SUBJECT,
                ConfirmationEmail.body(reservation.guestName(), reservation.document()));
        return reservation;
    }

    public synchronized Reservation cancel(String id) {
        Reservation reservation = find(id);
        reservation.cancel();
        return reservation;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation reservation = find(id);
        reservation.checkIn();
        return reservation;
    }

    public synchronized ReservationDocument checkOut(String id) {
        Reservation reservation = find(id);
        reservation.checkOut();
        return reservation.document();
    }

    public synchronized ReservationDocument document(String id) {
        return find(id).document();
    }

    public synchronized List<Reservation> occupying(LocalDate night) {
        return reservations.values().stream().filter(r -> r.occupies(night)).toList();
    }

    /** Reservas que começam no mês e deixaram dinheiro com a pousada. */
    public synchronized List<Reservation> retainedStartingIn(YearMonth month) {
        return new ArrayList<>(reservations.values().stream()
                .filter(r -> YearMonth.from(r.checkInDate()).equals(month))
                .filter(r -> r.retained().signum() > 0)
                .toList());
    }
}
