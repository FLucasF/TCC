package br.com.pousadamarealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {
    public enum Status { PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED }

    final long id;
    final Room room;
    final String guestName;
    final String guestEmail;
    final String guestCpf;
    final LocalDate checkIn;
    final LocalDate checkOut;
    final int guests;
    final RatePlan ratePlan;
    final int nights;
    final BigDecimal subtotal;
    final BigDecimal discount;
    final BigDecimal total;
    Status status = Status.PENDING;
    BigDecimal amountPaid = Money.ZERO;
    BigDecimal refund; // null until cancelled

    Reservation(long id, Room room, String guestName, String guestEmail, String guestCpf,
                LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        this.subtotal = Pricing.subtotal(room, checkIn, checkOut);
        this.discount = ratePlan.discountOn(subtotal);
        this.total = Money.round(subtotal.subtract(discount));
    }

    boolean active() {
        return status != Status.CANCELLED;
    }

    boolean paid() {
        return status == Status.CONFIRMED || status == Status.CHECKED_IN || status == Status.CHECKED_OUT;
    }

    boolean overlaps(LocalDate in, LocalDate out) {
        return checkIn.isBefore(out) && in.isBefore(checkOut);
    }

    /** Money the hotel kept: paid minus refunded. */
    BigDecimal netRevenue() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }
}
