package hotel.reservations;

import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.notifications.ReservationEventListener;
import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.util.List;

/** The life of a reservation: created, confirmed (paid), checked in, checked out, or cancelled. */
public final class ReservationService {

    static final String HOTEL_NAME = "Pousada Maré Alta";

    private final Rooms rooms;
    private final ReservationStore store;
    private final PriceCalculator priceCalculator;
    private final EmailSender emailSender;
    private final List<ReservationEventListener> eventListeners;

    public ReservationService(
            Rooms rooms,
            ReservationStore store,
            PriceCalculator priceCalculator,
            EmailSender emailSender,
            List<ReservationEventListener> eventListeners) {
        this.rooms = rooms;
        this.store = store;
        this.priceCalculator = priceCalculator;
        this.emailSender = emailSender;
        this.eventListeners = eventListeners;
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
        if (store.isRoomTaken(room.number(), request.checkIn(), request.checkOut())) {
            throw new HotelException(
                    ErrorCode.ROOM_UNAVAILABLE, "room " + room.number() + " is taken on some of those nights");
        }
        Price price = priceCalculator.calculate(room, request.checkIn(), request.checkOut(), request.ratePlan());
        return store.add(request, price);
    }

    public Reservation find(long reservationId) {
        return store.find(reservationId)
                .orElseThrow(() -> new HotelException(
                        ErrorCode.RESERVATION_NOT_FOUND, "reservation " + reservationId + " does not exist"));
    }

    /** The guest paid: the reservation is confirmed and the guest gets the receipt by e-mail. */
    public synchronized Reservation confirm(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.PENDING) {
            throw notAllowed(reservation, "confirm");
        }
        reservation.confirmPayment();
        store.update(reservation);
        ReservationReceipt receipt = new ReservationReceipt(reservation, roomOf(reservation));
        emailSender.send(
                reservation.guest().email(),
                "Reserva confirmada - " + HOTEL_NAME,
                confirmationEmailBody(reservation, receipt));
        for (ReservationEventListener listener : eventListeners) {
            listener.onReservationConfirmed(reservation);
        }
        return reservation;
    }

    /**
     * Cancelling before the stay starts. What was paid comes back according to the rate plan (a pending
     * reservation was never paid, so nothing comes back).
     */
    public synchronized Reservation cancel(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.PENDING
                && reservation.status() != ReservationStatus.CONFIRMED) {
            throw notAllowed(reservation, "cancel");
        }
        boolean wasPreviouslyConfirmed = reservation.status() == ReservationStatus.CONFIRMED;
        BigDecimal refund = switch (reservation.ratePlan()) {
            case STANDARD -> reservation.amountPaid();
            case NON_REFUNDABLE -> PriceCalculator.cents(BigDecimal.ZERO);
        };
        reservation.cancel(refund);
        store.update(reservation);
        for (ReservationEventListener listener : eventListeners) {
            listener.onReservationCancelled(reservation, wasPreviouslyConfirmed);
        }
        return reservation;
    }

    public synchronized Reservation checkIn(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.CONFIRMED) {
            throw notAllowed(reservation, "check in");
        }
        reservation.changeStatus(ReservationStatus.CHECKED_IN);
        store.update(reservation);
        return reservation;
    }

    /** The guest leaves: the stay is closed and the final receipt is handed over. */
    public synchronized ReservationReceipt checkOut(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.CHECKED_IN) {
            throw notAllowed(reservation, "check out");
        }
        reservation.changeStatus(ReservationStatus.CHECKED_OUT);
        store.update(reservation);
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

    private static String confirmationEmailBody(Reservation reservation, ReservationReceipt receipt) {
        return "Olá, " + reservation.guest().name() + "!\n\n"
                + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!\n";
    }

    private static HotelException notAllowed(Reservation reservation, String operation) {
        return new HotelException(
                ErrorCode.OPERATION_NOT_ALLOWED,
                "cannot " + operation + " reservation " + reservation.id() + " while it is " + reservation.status());
    }
}
