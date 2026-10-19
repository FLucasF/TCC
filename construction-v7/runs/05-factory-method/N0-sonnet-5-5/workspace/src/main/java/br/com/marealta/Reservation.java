package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

final class Reservation {
    final int id;
    final Room room;
    final String guestName;
    final String guestEmail;
    final Identification identification;
    final LocalDate checkIn;
    final LocalDate checkOut;
    final int guests;
    final RatePlan ratePlan;
    final Pricing.Quote quote;

    Status status = Status.PENDING;
    BigDecimal amountPaid = Money.ZERO;
    BigDecimal refund; // null until cancelled

    Reservation(int id, Room room, String guestName, String guestEmail, Identification identification,
                LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, Pricing.Quote quote) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.identification = identification;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.quote = quote;
    }

    boolean overlaps(LocalDate in, LocalDate out) {
        return checkIn.isBefore(out) && in.isBefore(checkOut);
    }

    /** What the pousada keeps: paid minus refunded. */
    BigDecimal netAmount() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }
}
