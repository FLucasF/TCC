package pousada;

import java.math.BigDecimal;

/** A reserva como o site a enxerga. */
public record ReservationView(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                              String checkIn, String checkOut, int guests, RatePlan ratePlan, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                              ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {

    public static ReservationView of(Reservation r) {
        return new ReservationView(r.id, r.room.number(), r.guestName, r.guestEmail, r.guestCpf,
                r.checkIn.toString(), r.checkOut.toString(), r.guests, r.ratePlan, r.nights,
                r.subtotal, r.discount, r.total, r.status(), r.amountPaid(), r.refund());
    }
}
