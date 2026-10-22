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
    private final RatePlan ratePlan;
    private final String companyCode;
    private final Price price;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    Reservation(long id, NewReservation request, Price price) {
        this(id, request, price, ReservationStatus.PENDING, PriceCalculator.cents(BigDecimal.ZERO), null);
    }

    Reservation(
            long id,
            NewReservation request,
            Price price,
            ReservationStatus status,
            BigDecimal amountPaid,
            BigDecimal refund) {
        this.id = id;
        this.roomNumber = request.roomNumber();
        this.guest = request.guest();
        this.checkIn = request.checkIn();
        this.checkOut = request.checkOut();
        this.guests = request.guests();
        this.ratePlan = request.ratePlan();
        this.companyCode = request.companyCode();
        this.price = price;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
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
}
