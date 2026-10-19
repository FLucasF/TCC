package pousada.domain;

import pousada.domain.Pricing.Quote;
import pousada.domain.document.Document;
import pousada.domain.identification.Identification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Casos de uso da pousada; guarda tudo em memória. */
public class ReservationService {

    private final List<Room> rooms;
    private final Outbox outbox;
    private final Map<Long, Reservation> reservations = new java.util.LinkedHashMap<>();

    public ReservationService(List<Room> rooms, Outbox outbox) {
        this.rooms = List.copyOf(rooms);
        this.outbox = outbox;
    }

    public List<Room> rooms() {
        return rooms;
    }

    public record NewReservation(int roomNumber, String guestName, String guestEmail, Identification identification,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
    }

    public synchronized Reservation create(NewReservation request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw DomainException.invalidRequest("A saída deve ser depois da entrada");
        }
        if (request.guests() < 1) {
            throw DomainException.invalidRequest("É preciso pelo menos um hóspede");
        }
        Room room = findRoom(request.roomNumber());
        if (request.guests() > room.capacity()) {
            throw DomainException.capacityExceeded(room.capacity());
        }
        boolean taken = reservations.values().stream()
                .anyMatch(r -> r.occupies(room.number(), request.checkIn(), request.checkOut()));
        if (taken) {
            throw DomainException.roomUnavailable(room.number());
        }
        Quote quote = Pricing.quote(room, request.checkIn(), request.checkOut(), request.ratePlan());
        Reservation reservation = new Reservation(reservations.size() + 1L, room, request.guestName(),
                request.guestEmail(), request.identification(), request.checkIn(), request.checkOut(),
                request.guests(), request.ratePlan(), quote);
        reservations.put(reservation.id(), reservation);
        return reservation;
    }

    public synchronized Reservation get(String id) {
        Reservation reservation = null;
        try {
            reservation = reservations.get(Long.parseLong(id));
        } catch (NumberFormatException ignored) {
            // id que não é número não existe
        }
        if (reservation == null) {
            throw DomainException.reservationNotFound(id);
        }
        return reservation;
    }

    public synchronized Reservation confirm(String id) {
        Reservation reservation = get(id);
        reservation.confirm();
        outbox.send(Message.Channel.EMAIL, reservation.guestEmail(), ConfirmationEmail.SUBJECT,
                ConfirmationEmail.body(reservation.guestName(), reservation.document()));
        return reservation;
    }

    public synchronized Reservation cancel(String id) {
        Reservation reservation = get(id);
        reservation.cancel();
        return reservation;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation reservation = get(id);
        reservation.startStay();
        return reservation;
    }

    public synchronized Document checkOut(String id) {
        Reservation reservation = get(id);
        reservation.endStay();
        return reservation.document();
    }

    public synchronized Document document(String id) {
        return get(id).document();
    }

    public synchronized List<Reservation> all() {
        return new ArrayList<>(reservations.values());
    }

    private Room findRoom(int number) {
        return rooms.stream().filter(r -> r.number() == number).findFirst()
                .orElseThrow(() -> DomainException.roomNotFound(number));
    }
}
