package br.com.maretalta.domain;

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
    public Status status = Status.PENDING;
    public BigDecimal amountPaid = Money.ZERO;
    /** null until cancelled. */
    public BigDecimal refund;

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
        this.nights = quote.nights();
        this.subtotal = quote.subtotal();
        this.discount = quote.discount();
        this.total = quote.total();
    }

    /** Money kept by the inn: paid minus refunded. */
    public BigDecimal netAmount() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }

    public boolean overlaps(LocalDate in, LocalDate out) {
        return checkIn.isBefore(out) && in.isBefore(checkOut);
    }
}
