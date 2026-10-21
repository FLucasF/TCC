package br.com.marealta.reservas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private final int id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Pricing.Quote quote;
    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = Money.ZERO;
    private BigDecimal refund;

    public Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       Pricing.Quote quote) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.quote = quote;
    }

    public int id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public int nights() { return quote.nights(); }
    public BigDecimal subtotal() { return quote.subtotal(); }
    public BigDecimal discount() { return quote.discount(); }
    public BigDecimal total() { return quote.total(); }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    /** Null enquanto a reserva não for cancelada. */
    public BigDecimal refund() { return refund; }

    /** Dinheiro que ficou com a pousada: pago menos devolvido. */
    public BigDecimal amountKept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    /** Ocupa o quarto na noite informada (entrada inclusive, saída exclusive). */
    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    private void require(String operation, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw new BookingException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Não é possível fazer " + operation + " com a reserva " + status);
    }

    public void confirm() {
        require("a confirmação", ReservationStatus.PENDING);
        amountPaid = total();
        status = ReservationStatus.CONFIRMED;
    }

    public void checkInGuest() {
        require("o check-in", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void checkOutGuest() {
        require("o check-out", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        require("o cancelamento", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refundable() ? amountPaid : Money.ZERO;
        status = ReservationStatus.CANCELLED;
    }
}
