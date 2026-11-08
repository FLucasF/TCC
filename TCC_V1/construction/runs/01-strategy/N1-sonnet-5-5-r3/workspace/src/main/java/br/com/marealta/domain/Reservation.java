package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import br.com.marealta.domain.DomainException.Code;

public class Reservation {
    private final long id;
    private final int roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final String ratePlan;
    private final String companyCode;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, String companyCode,
                       int nights, BigDecimal subtotal, BigDecimal discount,
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
        this.companyCode = companyCode;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public long id() { return id; }
    public int roomNumber() { return roomNumber; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public String ratePlan() { return ratePlan; }
    public String companyCode() { return companyCode; }
    public int nights() { return nights; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal discount() { return discount; }
    public BigDecimal total() { return subtotal.subtract(discount); }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    /** Null enquanto a reserva não for cancelada. */
    public BigDecimal refund() { return refund; }

    public void confirm() {
        moveTo(ReservationStatus.CONFIRMED, Set.of(ReservationStatus.PENDING));
        amountPaid = total();
    }

    public void registerCheckIn() {
        moveTo(ReservationStatus.CHECKED_IN, Set.of(ReservationStatus.CONFIRMED));
    }

    public void registerCheckOut() {
        moveTo(ReservationStatus.CHECKED_OUT, Set.of(ReservationStatus.CHECKED_IN));
    }

    /** @param refundOfPaid quanto devolver do valor pago, conforme a regra do plano de tarifa. */
    public void cancel(java.util.function.UnaryOperator<BigDecimal> refundOfPaid) {
        moveTo(ReservationStatus.CANCELLED, Set.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
        refund = refundOfPaid.apply(amountPaid);
    }

    private void moveTo(ReservationStatus target, Set<ReservationStatus> allowedFrom) {
        if (!allowedFrom.contains(status)) {
            throw new DomainException(Code.OPERATION_NOT_ALLOWED,
                    "Operação não permitida para reserva com status " + status);
        }
        status = target;
    }
}
