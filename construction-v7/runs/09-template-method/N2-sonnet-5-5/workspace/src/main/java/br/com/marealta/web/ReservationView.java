package br.com.marealta.web;

import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;

public record ReservationView(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                              String checkIn, String checkOut, int guests, String ratePlan, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                              BigDecimal amountPaid, BigDecimal refund) {
    static ReservationView of(Reservation r) {
        return new ReservationView(r.id(), r.room().number(), r.guestName(), r.guestEmail(), r.guestCpf(),
                r.checkInDate().toString(), r.checkOutDate().toString(), r.guests(), r.ratePlan().name(),
                r.nights(), r.subtotal(), r.discount(), r.total(), r.status().name(), r.amountPaid(), r.refund());
    }
}
