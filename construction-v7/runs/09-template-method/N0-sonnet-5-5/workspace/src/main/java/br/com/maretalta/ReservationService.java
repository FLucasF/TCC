package br.com.maretalta;

import br.com.maretalta.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ReservationService {
    public static final String EMAIL_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    public record Request(int roomNumber, String guestName, String guestEmail, String guestCpf,
                          LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    private final List<Room> rooms = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));
    private final List<Reservation> reservations = new ArrayList<>();
    private final Outbox outbox;

    public ReservationService(Outbox outbox) {
        this.outbox = outbox;
    }

    public List<Room> rooms() {
        return rooms;
    }

    public synchronized Reservation create(Request req) {
        if (!req.checkOut().isAfter(req.checkIn())) {
            throw ApiException.invalid("checkOut must be after checkIn");
        }
        if (req.guests() < 1) {
            throw ApiException.invalid("guests must be at least 1");
        }
        Room room = rooms.stream().filter(r -> r.number() == req.roomNumber()).findFirst()
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Room " + req.roomNumber() + " not found"));
        if (req.guests() > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "Room " + room.number() + " holds at most " + room.capacity() + " guests");
        }
        boolean taken = reservations.stream().anyMatch(r -> r.room.equals(room)
                && r.status != Status.CANCELLED && r.overlaps(req.checkIn(), req.checkOut()));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Room " + room.number() + " is not available for these dates");
        }
        Pricing.Quote quote = Pricing.quote(room, req.checkIn(), req.checkOut(), req.ratePlan());
        Reservation r = new Reservation(reservations.size() + 1, room, req.guestName(), req.guestEmail(),
                req.guestCpf(), req.checkIn(), req.checkOut(), req.guests(), req.ratePlan(), quote);
        reservations.add(r);
        return r;
    }

    public synchronized Reservation get(String id) {
        try {
            int n = Integer.parseInt(id);
            if (n >= 1 && n <= reservations.size()) {
                return reservations.get(n - 1);
            }
        } catch (NumberFormatException ignored) {
            // falls through to not found
        }
        throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation " + id + " not found");
    }

    public synchronized Reservation confirm(String id) {
        Reservation r = get(id);
        requireStatus(r, "confirmed", Status.PENDING);
        r.status = Status.CONFIRMED;
        r.amountPaid = r.total;
        outbox.send(Outbox.Channel.EMAIL, r.guestEmail, EMAIL_SUBJECT, ReceiptFormatter.confirmationEmail(r));
        return r;
    }

    public synchronized Reservation cancel(String id) {
        Reservation r = get(id);
        requireStatus(r, "cancelled", Status.PENDING, Status.CONFIRMED);
        r.refund = r.ratePlan.refundable() ? r.amountPaid : Money.ZERO;
        r.status = Status.CANCELLED;
        return r;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation r = get(id);
        requireStatus(r, "checked in", Status.CONFIRMED);
        r.status = Status.CHECKED_IN;
        return r;
    }

    public synchronized Reservation checkOut(String id) {
        Reservation r = get(id);
        requireStatus(r, "checked out", Status.CHECKED_IN);
        r.status = Status.CHECKED_OUT;
        return r;
    }

    /** Receipt copy: only for paid reservations. */
    public synchronized Reservation receiptFor(String id) {
        Reservation r = get(id);
        if (!r.status.isPaid()) {
            throw ApiException.notAllowed("Reservation " + r.id + " is " + r.status + " and has no receipt");
        }
        return r;
    }

    /** Runs a read-only computation over a consistent view of all reservations. */
    public synchronized <T> T read(Function<List<Reservation>, T> fn) {
        return fn.apply(List.copyOf(reservations));
    }

    private void requireStatus(Reservation r, String action, Status... allowed) {
        for (Status s : allowed) {
            if (r.status == s) return;
        }
        throw ApiException.notAllowed("Reservation " + r.id + " is " + r.status + " and cannot be " + action);
    }
}
