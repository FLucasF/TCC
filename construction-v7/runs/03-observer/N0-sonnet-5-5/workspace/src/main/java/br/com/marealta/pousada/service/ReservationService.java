package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Pricing;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.domain.RoomType;
import br.com.marealta.pousada.event.EventBus;
import br.com.marealta.pousada.event.ReservationEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
    }

    private final EventBus events;
    private final List<Room> rooms = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));
    private final Map<Long, Reservation> reservations = new LinkedHashMap<>();
    private long nextId = 1;

    public ReservationService(EventBus events) {
        this.events = events;
    }

    public List<Room> rooms() {
        return rooms;
    }

    public synchronized Reservation create(NewReservation req) {
        if (!req.checkOut().isAfter(req.checkIn())) {
            throw DomainException.invalid("A saída deve ser depois da entrada");
        }
        if (req.guests() < 1) {
            throw DomainException.invalid("É preciso pelo menos um hóspede");
        }
        Room room = rooms.stream().filter(r -> r.number() == req.roomNumber()).findFirst()
                .orElseThrow(() -> new DomainException(404, "ROOM_NOT_FOUND", "Quarto não encontrado"));
        if (req.guests() > room.capacity()) {
            throw new DomainException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        boolean taken = reservations.values().stream()
                .anyMatch(r -> r.room().equals(room) && r.status() != ReservationStatus.CANCELLED
                        && r.overlaps(req.checkIn(), req.checkOut()));
        if (taken) {
            throw new DomainException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
        }
        Pricing.Quote quote = Pricing.quote(room, req.checkIn(), req.checkOut(), req.ratePlan());
        Reservation reservation = new Reservation(nextId++, room, req.guestName(), req.guestEmail(), req.guestCpf(),
                req.checkIn(), req.checkOut(), req.guests(), req.ratePlan(), quote.nights(), quote.subtotal(),
                quote.discount(), quote.total(), ReservationStatus.PENDING, Money.ZERO, null);
        reservations.put(reservation.id(), reservation);
        return reservation;
    }

    public synchronized Reservation get(long id) {
        Reservation r = reservations.get(id);
        if (r == null) {
            throw notFound();
        }
        return r;
    }

    public synchronized List<Reservation> all() {
        return new ArrayList<>(reservations.values());
    }

    public synchronized Reservation confirm(long id) {
        Reservation current = requireStatus(id, "confirmar", ReservationStatus.PENDING);
        Reservation confirmed = current.confirmed();
        reservations.put(id, confirmed);
        events.publish(new ReservationEvent.Confirmed(confirmed));
        return confirmed;
    }

    public synchronized Reservation checkIn(long id) {
        Reservation current = requireStatus(id, "fazer check-in de", ReservationStatus.CONFIRMED);
        Reservation updated = current.withStatus(ReservationStatus.CHECKED_IN);
        reservations.put(id, updated);
        return updated;
    }

    public synchronized Receipt checkOut(long id) {
        Reservation current = requireStatus(id, "fazer check-out de", ReservationStatus.CHECKED_IN);
        Reservation updated = current.withStatus(ReservationStatus.CHECKED_OUT);
        reservations.put(id, updated);
        return Receipt.of(updated);
    }

    public synchronized Reservation cancel(long id) {
        Reservation current = requireStatus(id, "cancelar", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        boolean wasConfirmed = current.status() == ReservationStatus.CONFIRMED;
        BigDecimal refund = wasConfirmed && current.ratePlan().refundable() ? current.amountPaid() : Money.ZERO;
        Reservation cancelled = current.cancelled(refund);
        reservations.put(id, cancelled);
        events.publish(new ReservationEvent.Cancelled(cancelled, wasConfirmed));
        return cancelled;
    }

    public synchronized Receipt document(long id) {
        Reservation r = get(id);
        if (!r.status().paid()) {
            throw new DomainException(409, "OPERATION_NOT_ALLOWED",
                    "A segunda via só existe para reserva paga");
        }
        return Receipt.of(r);
    }

    private Reservation requireStatus(long id, String action, ReservationStatus... allowed) {
        Reservation r = get(id);
        for (ReservationStatus s : allowed) {
            if (r.status() == s) {
                return r;
            }
        }
        throw new DomainException(409, "OPERATION_NOT_ALLOWED",
                "Não é possível " + action + " uma reserva com situação " + r.status());
    }

    private static DomainException notFound() {
        return new DomainException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
    }
}
