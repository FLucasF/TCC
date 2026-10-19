package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    public final int id;
    public final Room room;
    public final String guestName;
    public final String guestEmail;
    public final String guestCpf;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final int guests;
    public final RatePlan ratePlan;
    public final int nights;
    public final BigDecimal subtotal;
    public final BigDecimal discount;
    public final BigDecimal total;

    public ReservationStatus status = ReservationStatus.PENDING;
    public BigDecimal amountPaid = Money.round(BigDecimal.ZERO);
    public BigDecimal refund; // null até ser cancelada

    public Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       Pricing.Quote q) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = q.nights();
        this.subtotal = q.subtotal();
        this.discount = q.discount();
        this.total = q.total();
    }

    public boolean overlaps(LocalDate in, LocalDate out) {
        return checkIn.isBefore(out) && in.isBefore(checkOut);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal netAmount() {
        return Money.round(amountPaid.subtract(refund == null ? BigDecimal.ZERO : refund));
    }
}
