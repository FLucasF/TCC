package br.com.marealta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final List<Reservation> reservations = new ArrayList<>();
    private final Outbox outbox;

    public ReservationService(Outbox outbox) {
        this.outbox = outbox;
    }

    public List<Room> rooms() {
        return Room.ALL;
    }

    public synchronized Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                           LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalid("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw ApiException.invalid("É preciso pelo menos um hóspede");
        }
        Room room = Room.ALL.stream().filter(r -> r.number() == roomNumber).findFirst()
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + roomNumber + " não existe"));
        if (guests > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + roomNumber + " comporta até " + room.capacity() + " pessoas");
        }
        boolean taken = reservations.stream()
                .filter(r -> r.status() != ReservationStatus.CANCELLED)
                .filter(r -> r.room().equals(room))
                .anyMatch(r -> r.overlaps(checkIn, checkOut));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto " + roomNumber + " já reservado nesse período");
        }
        Reservation reservation = new Reservation(reservations.size() + 1, room, guestName, guestEmail,
                guestCpf, checkIn, checkOut, guests, ratePlan);
        reservations.add(reservation);
        return reservation;
    }

    public synchronized Reservation find(int id) {
        return findOptional(id).orElseThrow(
                () -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe"));
    }

    private Optional<Reservation> findOptional(int id) {
        return id >= 1 && id <= reservations.size() ? Optional.of(reservations.get(id - 1)) : Optional.empty();
    }

    public synchronized List<Reservation> all() {
        return List.copyOf(reservations);
    }

    public synchronized Reservation confirm(int id) {
        Reservation r = require(id, ReservationStatus.PENDING, "confirmada");
        r.confirm();
        Receipt receipt = Receipt.of(r);
        outbox.send(Channel.EMAIL, r.guestEmail(), CONFIRMATION_SUBJECT,
                "Olá, " + r.guestName() + "!\n\n"
                        + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                        + receipt.text() + "\n\n"
                        + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                        + "Até breve!");
        return r;
    }

    public synchronized Reservation checkIn(int id) {
        Reservation r = require(id, ReservationStatus.CONFIRMED, "objeto de check-in");
        r.markCheckedIn();
        return r;
    }

    public synchronized Receipt checkOut(int id) {
        Reservation r = require(id, ReservationStatus.CHECKED_IN, "objeto de check-out");
        r.markCheckedOut();
        return Receipt.of(r);
    }

    public synchronized Reservation cancel(int id) {
        Reservation r = find(id);
        if (r.status() != ReservationStatus.PENDING && r.status() != ReservationStatus.CONFIRMED) {
            throw notAllowed(r, "cancelada");
        }
        r.cancel();
        return r;
    }

    /** Segunda via: só existe para reserva paga. */
    public synchronized Receipt document(int id) {
        Reservation r = find(id);
        if (!r.status().isPaid()) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                    "Reserva " + id + " (" + r.status() + ") não tem comprovante");
        }
        return Receipt.of(r);
    }

    private Reservation require(int id, ReservationStatus expected, String action) {
        Reservation r = find(id);
        if (r.status() != expected) {
            throw notAllowed(r, action);
        }
        return r;
    }

    private static ApiException notAllowed(Reservation r, String action) {
        return new ApiException(409, "OPERATION_NOT_ALLOWED",
                "Reserva " + r.id() + " está " + r.status() + " e não pode ser " + action);
    }
}
