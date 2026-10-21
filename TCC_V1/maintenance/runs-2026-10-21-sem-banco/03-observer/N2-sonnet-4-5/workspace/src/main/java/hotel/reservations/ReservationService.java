package hotel.reservations;

import hotel.documents.ReservationReceipt;
import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.util.List;

/** The life of a reservation: created, confirmed (paid), checked in, checked out, or cancelled. */
public final class ReservationService {

    private final Rooms rooms;
    private final ReservationStore store;
    private final PriceCalculator priceCalculator;
    private final List<ReservationListener> listeners;

    public ReservationService(
            Rooms rooms, ReservationStore store, PriceCalculator priceCalculator, List<ReservationListener> listeners) {
        this.rooms = rooms;
        this.store = store;
        this.priceCalculator = priceCalculator;
        this.listeners = List.copyOf(listeners);
    }

    public synchronized Reservation create(NewReservation request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "check-out must be after check-in");
        }
        if (request.guests() < 1) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "a reservation needs at least one guest");
        }
        Room room = rooms.find(request.roomNumber())
                .orElseThrow(() -> new HotelException(
                        ErrorCode.ROOM_NOT_FOUND, "room " + request.roomNumber() + " does not exist"));
        if (request.guests() > room.capacity()) {
            throw new HotelException(
                    ErrorCode.CAPACITY_EXCEEDED,
                    "room " + room.number() + " takes at most " + room.capacity() + " guests");
        }
        boolean roomTaken = store.all().stream()
                .filter(existing -> existing.status() != ReservationStatus.CANCELLED)
                .anyMatch(existing -> existing.overlaps(room.number(), request.checkIn(), request.checkOut()));
        if (roomTaken) {
            throw new HotelException(
                    ErrorCode.ROOM_UNAVAILABLE, "room " + room.number() + " is taken on some of those nights");
        }
        Price price = priceCalculator.calculate(room, request.checkIn(), request.checkOut(), request.ratePlan());
        Reservation reservation = new Reservation(store.nextId(), request, price);
        store.save(reservation);
        return reservation;
    }

    public Reservation find(long reservationId) {
        return store.find(reservationId)
                .orElseThrow(() -> new HotelException(
                        ErrorCode.RESERVATION_NOT_FOUND, "reservation " + reservationId + " does not exist"));
    }

    /** The guest paid: the reservation is confirmed and listeners are notified. */
    public synchronized Reservation confirm(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.PENDING) {
            throw notAllowed(reservation, "confirm");
        }
        reservation.confirmPayment();
        Room room = roomOf(reservation);
        for (ReservationListener listener : listeners) {
            listener.onReservationConfirmed(reservation, room);
        }
        return reservation;
    }

    /**
     * Cancelling before the stay starts. What was paid comes back according to the rate plan (a pending
     * reservation was never paid, so nothing comes back). Listeners are notified.
     */
    public synchronized Reservation cancel(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.PENDING
                && reservation.status() != ReservationStatus.CONFIRMED) {
            throw notAllowed(reservation, "cancel");
        }
        BigDecimal refund = switch (reservation.ratePlan()) {
            case STANDARD -> reservation.amountPaid();
            case NON_REFUNDABLE -> PriceCalculator.cents(BigDecimal.ZERO);
        };
        reservation.cancel(refund);
        Room room = roomOf(reservation);
        for (ReservationListener listener : listeners) {
            listener.onReservationCancelled(reservation, room);
        }
        return reservation;
    }

    public synchronized Reservation checkIn(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.CONFIRMED) {
            throw notAllowed(reservation, "check in");
        }
        reservation.changeStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    /** The guest leaves: the stay is closed and the final receipt is handed over. */
    public synchronized ReservationReceipt checkOut(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.CHECKED_IN) {
            throw notAllowed(reservation, "check out");
        }
        reservation.changeStatus(ReservationStatus.CHECKED_OUT);
        return new ReservationReceipt(reservation, roomOf(reservation));
    }

    /** A copy of the receipt, for a guest who lost it. Only paid reservations have one. */
    public ReservationReceipt receiptCopy(long reservationId) {
        Reservation reservation = find(reservationId);
        if (!reservation.isPaid()) {
            throw notAllowed(reservation, "issue a receipt for");
        }
        return new ReservationReceipt(reservation, roomOf(reservation));
    }

    private Room roomOf(Reservation reservation) {
        return rooms.find(reservation.roomNumber()).orElseThrow();
    }

    private static HotelException notAllowed(Reservation reservation, String operation) {
        return new HotelException(
                ErrorCode.OPERATION_NOT_ALLOWED,
                "cannot " + operation + " reservation " + reservation.id() + " while it is " + reservation.status());
    }
}
