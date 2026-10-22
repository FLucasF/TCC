package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import static br.com.marealta.domain.ReservationStatus.*;

public class Reservation {
    private final long id;
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

    public Reservation(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       Pricing.Quote quote, ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = quote.nights();
        this.subtotal = quote.subtotal();
        this.discount = quote.discount();
        this.total = quote.total();
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public void confirm() {
        require(status == PENDING, "Só é possível confirmar uma reserva pendente");
        status = CONFIRMED;
        amountPaid = total;
    }

    public void checkIn() {
        require(status == CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        status = CHECKED_IN;
    }

    public void checkOut() {
        require(status == CHECKED_IN, "Só é possível fazer check-out de uma reserva com check-in");
        status = CHECKED_OUT;
    }

    public void cancel() {
        require(status == PENDING || status == CONFIRMED, "Só é possível cancelar antes do check-in");
        status = CANCELLED;
        refund = ratePlan.refund(amountPaid);
    }

    public void requirePaid() {
        require(status.isPaid(), "O comprovante só existe para reserva paga");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw ApiException.notAllowed(message);
    }

    /** O que fica com a pousada: pago menos devolvido. */
    public BigDecimal retainedAmount() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public long id() { return id; }
    public int roomNumber() { return roomNumber; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkInDate() { return checkIn; }
    public LocalDate checkOutDate() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public int nights() { return nights; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal discount() { return discount; }
    public BigDecimal total() { return total; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
