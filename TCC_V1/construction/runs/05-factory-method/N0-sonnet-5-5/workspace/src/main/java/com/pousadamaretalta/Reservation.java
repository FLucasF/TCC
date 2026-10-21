package com.pousadamaretalta;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    public final int id;
    public final Room room;
    public final String guestName;
    public final String guestEmail;
    public final GuestIdentity identity;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final int guests;
    public final RatePlan ratePlan;
    public final Pricing.Quote quote;

    ReservationStatus status = ReservationStatus.PENDING;
    BigDecimal amountPaid = Money.ZERO;
    BigDecimal refund; // null até o cancelamento

    Reservation(int id, Room room, String guestName, String guestEmail, GuestIdentity identity,
                LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, Pricing.Quote quote) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.identity = identity;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.quote = quote;
    }

    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal netAmount() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }

    /** Ocupa a noite (entra nela; quem sai naquele dia não conta). */
    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean overlaps(LocalDate in, LocalDate out) {
        return checkIn.isBefore(out) && in.isBefore(checkOut);
    }

    public ReservationDocument document() {
        return new ReservationDocument(id, guestName, identity, room.number(), room.type(),
                checkIn, checkOut, quote.nights(), quote.subtotal(), quote.discount(), quote.total());
    }
}
