package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Forma da reserva no JSON do site. */
public record ReservationView(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                              ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {

    static ReservationView of(Reservation r) {
        return new ReservationView(r.id(), r.room().number(), r.guestName(), r.guestEmail(), r.guestCpf(),
                r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(),
                r.total(), r.status(), r.amountPaid(), r.refund());
    }
}
