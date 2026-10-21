package br.com.marealta.domain;

import br.com.marealta.plan.RatePlan;
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
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = Money.ZERO;
    private BigDecimal refund; // null até o cancelamento

    public Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       Quote quote) {
        this.id = id;
        this.room = room;
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
    }

    /** Ocupa o quarto em alguma noite do intervalo [from, to)? */
    public boolean overlaps(LocalDate from, LocalDate to) {
        return checkIn.isBefore(to) && from.isBefore(checkOut);
    }

    public void confirm() {
        status = ReservationStatus.CONFIRMED;
        amountPaid = total;
    }

    public void startStay() { status = ReservationStatus.CHECKED_IN; }

    public void finishStay() { status = ReservationStatus.CHECKED_OUT; }

    public void cancel() {
        refund = ratePlan.refund(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    /** Dinheiro que ficou com a pousada: pago menos devolvido. */
    public BigDecimal netRevenue() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
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
    public int nights() { return nights; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal discount() { return discount; }
    public BigDecimal total() { return total; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
