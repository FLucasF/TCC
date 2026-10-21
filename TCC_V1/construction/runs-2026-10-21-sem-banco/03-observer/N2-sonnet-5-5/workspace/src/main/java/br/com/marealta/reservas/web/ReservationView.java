package br.com.marealta.reservas.web;

import br.com.marealta.reservas.domain.RatePlan;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.ReservationStatus;

import java.math.BigDecimal;

public record ReservationView(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                              String checkIn, String checkOut, int guests, RatePlan ratePlan, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                              ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {

    static ReservationView of(Reservation r) {
        return new ReservationView(r.id(), r.room().number(), r.guestName(), r.guestEmail(), r.guestCpf(),
                r.checkIn().toString(), r.checkOut().toString(), r.guests(), r.ratePlan(), r.nights(),
                r.subtotal(), r.discount(), r.total(), r.status(), r.amountPaid(), r.refund());
    }
}
