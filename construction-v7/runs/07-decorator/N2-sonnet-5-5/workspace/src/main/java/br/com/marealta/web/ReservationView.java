package br.com.marealta.web;

import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationView(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                              BigDecimal amountPaid, BigDecimal refund) {

    public static ReservationView of(Reservation r) {
        return new ReservationView(r.id(), r.room().number(), r.guestName(), r.guestEmail(), r.guestCpf(),
                r.checkIn(), r.checkOut(), r.guests(), r.ratePlan().name(), r.quote().nights(),
                r.quote().subtotal(), r.quote().discount(), r.quote().total(), r.status().name(),
                r.amountPaid(), r.refund());
    }
}
