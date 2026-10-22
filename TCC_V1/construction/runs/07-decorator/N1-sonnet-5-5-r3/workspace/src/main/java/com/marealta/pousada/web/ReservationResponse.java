package com.marealta.pousada.web;

import com.marealta.pousada.domain.RatePlan;
import com.marealta.pousada.domain.Reservation;
import com.marealta.pousada.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

record ReservationResponse(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                           LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
                           BigDecimal subtotal, BigDecimal discount, BigDecimal total, ReservationStatus status,
                           BigDecimal amountPaid, BigDecimal refund) {
    static ReservationResponse of(Reservation r) {
        return new ReservationResponse(r.id(), r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(),
                r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.quote().nights(), r.quote().subtotal(),
                r.quote().discount(), r.quote().total(), r.status(), r.amountPaid(), r.refund());
    }
}
