package br.com.marealta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ReservationService {
    private final Rooms rooms;
    private final Outbox outbox;
    private final List<Reservation> reservations = new ArrayList<>();

    public ReservationService(Rooms rooms, Outbox outbox) {
        this.rooms = rooms;
        this.outbox = outbox;
    }

    public synchronized Reservation create(int roomNumber, String name, String email, String cpf,
                                           LocalDate checkIn, LocalDate checkOut, int guests, RatePlan plan) {
        Room room = rooms.find(roomNumber)
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + roomNumber + " não existe"));
        if (guests > room.type().capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + roomNumber + " comporta no máximo " + room.type().capacity() + " hóspedes");
        }
        boolean taken = reservations.stream().anyMatch(r -> r.getRoomNumber() == roomNumber
                && r.getStatus() != ReservationStatus.CANCELLED && r.overlaps(checkIn, checkOut));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
        }
        Reservation reservation = new Reservation(reservations.size() + 1L, room, name, email, cpf,
                checkIn, checkOut, guests, plan);
        reservations.add(reservation);
        return reservation;
    }

    public synchronized Reservation get(String id) {
        return find(id).orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada"));
    }

    private Optional<Reservation> find(String id) {
        try {
            long number = Long.parseLong(id);
            return reservations.stream().filter(r -> r.getId() == number).findFirst();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public synchronized Reservation confirm(String id) {
        Reservation reservation = get(id);
        reservation.confirm();
        outbox.sendEmail(reservation.getGuestEmail(), "Reserva confirmada - Pousada Maré Alta", emailBody(reservation));
        return reservation;
    }

    public synchronized Reservation cancel(String id) {
        Reservation reservation = get(id);
        reservation.cancel();
        return reservation;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation reservation = get(id);
        reservation.checkIn();
        return reservation;
    }

    public synchronized Receipt checkOut(String id) {
        Reservation reservation = get(id);
        reservation.checkOut();
        return Receipt.of(reservation);
    }

    public synchronized Receipt document(String id) {
        Reservation reservation = get(id);
        if (!reservation.getStatus().isPaid()) {
            throw ApiException.notAllowed("Segunda via só existe para reserva paga");
        }
        return Receipt.of(reservation);
    }

    public synchronized List<Reservation> select(Predicate<Reservation> filter) {
        return reservations.stream().filter(filter).toList();
    }

    private static String emailBody(Reservation r) {
        return "Olá, " + r.getGuestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(r).toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
