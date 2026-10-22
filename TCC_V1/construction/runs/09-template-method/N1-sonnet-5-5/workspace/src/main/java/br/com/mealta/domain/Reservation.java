package br.com.mealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {
    private Long id;
    private final int roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                       ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public static Reservation pending(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                      Quote quote) {
        return new Reservation(null, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, quote.subtotal(), quote.discount(), quote.total(),
                ReservationStatus.PENDING, Money.ZERO, null);
    }

    public void confirm() {
        moveTo(ReservationStatus.CONFIRMED);
        amountPaid = total;
    }

    public void checkIn() {
        moveTo(ReservationStatus.CHECKED_IN);
    }

    public void checkOut() {
        moveTo(ReservationStatus.CHECKED_OUT);
    }

    public void cancel() {
        moveTo(ReservationStatus.CANCELLED);
        refund = ratePlan.refundOf(amountPaid);
    }

    private void moveTo(ReservationStatus next) {
        if (!status.canGoTo(next)) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Operação não permitida para reserva com status " + status);
        }
        status = next;
    }

    public boolean isPaid() {
        return status.isPaid();
    }

    /** O dinheiro que fica com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal keptAmount() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    public boolean retainsMoney() {
        return keptAmount().signum() > 0;
    }

    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public Long id() { return id; }
    public void assignId(long id) { this.id = id; }
    public int roomNumber() { return roomNumber; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkInDate() { return checkIn; }
    public LocalDate checkOutDate() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal discount() { return discount; }
    public BigDecimal total() { return total; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
