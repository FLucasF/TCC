package br.com.mare.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private final Long id;
    private final int roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
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
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public void confirm() {
        moveTo(ReservationStatus.CONFIRMED);
        amountPaid = total;
    }

    public void registerCheckIn() {
        moveTo(ReservationStatus.CHECKED_IN);
    }

    public void registerCheckOut() {
        moveTo(ReservationStatus.CHECKED_OUT);
    }

    public void cancel() {
        moveTo(ReservationStatus.CANCELLED);
        refund = ratePlan.refundOn(amountPaid);
    }

    /** O dinheiro que fica com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal retainedAmount() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    private void moveTo(ReservationStatus target) {
        if (!status.canMoveTo(target)) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Reserva " + id + " está " + status + " e não pode passar para " + target);
        }
        status = target;
    }

    public Long getId() { return id; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getGuestCpf() { return guestCpf; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public RatePlan getRatePlan() { return ratePlan; }
    public int getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public ReservationStatus getStatus() { return status; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getRefund() { return refund; }
}
