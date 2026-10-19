package br.com.mare;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {
    final int id;
    final Room room;
    final String guestName, guestEmail, guestCpf;
    final LocalDate checkIn, checkOut;
    final int guests;
    final RatePlan plan;
    final String companyCode;
    final BigDecimal subtotal, discount, total;
    ReservationStatus status = ReservationStatus.PENDING;
    BigDecimal amountPaid = Money.ZERO;
    BigDecimal refund; // null até cancelar

    Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf,
                LocalDate checkIn, LocalDate checkOut, int guests, RatePlan plan,
                String companyCode, RatePlan.Quote quote) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.plan = plan;
        this.companyCode = companyCode;
        this.subtotal = quote.subtotal();
        this.discount = quote.discount();
        this.total = Money.round(quote.total());
    }

    int nights() { return (int) ChronoUnit.DAYS.between(checkIn, checkOut); }

    /** Dinheiro que ficou com a pousada. */
    BigDecimal netAmount() {
        return amountPaid.subtract(refund == null ? Money.ZERO : refund);
    }

    boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }
}
