package hotel.reservations;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Reservation {

    private final long id;
    private final int roomNumber;
    private final Guest guest;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final int children;
    private final RatePlan ratePlan;
    private final Price price;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    Reservation(long id, NewReservation request, Price price) {
        this.id = id;
        this.roomNumber = request.roomNumber();
        this.guest = request.guest();
        this.checkIn = request.checkIn();
        this.checkOut = request.checkOut();
        this.guests = request.guests();
        this.children = request.children();
        this.ratePlan = request.ratePlan();
        this.price = price;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = PriceCalculator.cents(BigDecimal.ZERO);
    }

    public long id() {
        return id;
    }

    public int roomNumber() {
        return roomNumber;
    }

    public Guest guest() {
        return guest;
    }

    public LocalDate checkIn() {
        return checkIn;
    }

    public LocalDate checkOut() {
        return checkOut;
    }

    public int guests() {
        return guests;
    }

    /** Children up to 12 years old: they take room space, but do not pay. */
    public int children() {
        return children;
    }

    public RatePlan ratePlan() {
        return ratePlan;
    }

    public Price price() {
        return price;
    }

    public ReservationStatus status() {
        return status;
    }

    /** What the guest paid: the total, once the reservation is confirmed; zero before that. */
    public BigDecimal amountPaid() {
        return amountPaid;
    }

    /** What the guest got back when the reservation was cancelled; null while it is not cancelled. */
    public BigDecimal refund() {
        return refund;
    }

    /** Confirmed, checked in or checked out: the guest paid and the room is theirs. */
    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED
                || status == ReservationStatus.CHECKED_IN
                || status == ReservationStatus.CHECKED_OUT;
    }

    /** The money the inn keeps from this reservation. */
    public BigDecimal amountKept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    void changeStatus(ReservationStatus newStatus) {
        this.status = newStatus;
    }

    void confirmPayment() {
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = price.total();
    }

    void cancel(BigDecimal refundAmount) {
        this.status = ReservationStatus.CANCELLED;
        this.refund = refundAmount;
    }

    boolean overlaps(int otherRoomNumber, LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return roomNumber == otherRoomNumber
                && checkIn.isBefore(otherCheckOut)
                && otherCheckIn.isBefore(checkOut);
    }
}
