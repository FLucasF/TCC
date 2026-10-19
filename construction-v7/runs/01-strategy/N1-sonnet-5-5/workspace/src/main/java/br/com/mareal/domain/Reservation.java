package br.com.mareal.domain;

import br.com.mareal.plans.RatePlan;
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
    public BigDecimal amountPaid = Money.ZERO;
    public BigDecimal refund; // null until cancelled

    public Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        this.id = id;
        this.room = room;
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
    }

    public boolean overlaps(LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return checkIn.isBefore(otherCheckOut) && otherCheckIn.isBefore(checkOut);
    }

    /** Whether the guest occupies the room on the given night (leaving on that day does not count). */
    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    /** Money that stayed with the inn: paid minus refunded. */
    public BigDecimal netAmount() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }
}
