package br.com.pousadamarealta.reservas.web;

import br.com.pousadamarealta.reservas.domain.RatePlan;
import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationView(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                              ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {

    public static ReservationView of(Reservation r) {
        return new ReservationView(r.id(), r.room().number(), r.guestName(), r.guestEmail(), r.guestCpf(),
                r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.quote().nights(),
                r.quote().subtotal(), r.quote().discount(), r.quote().total(), r.status(),
                r.amountPaid(), r.refund());
    }
}
