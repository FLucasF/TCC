package br.com.marealta.pousada.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;

public record ReservationView(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                              ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {

    public static ReservationView of(Reservation r) {
        return new ReservationView(r.id(), r.room().number(), r.guest().name(), r.guest().email(),
                r.guest().cpf(), r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.quote().nights(),
                r.quote().subtotal(), r.quote().discount(), r.quote().total(), r.status(),
                r.amountPaid(), r.refund());
    }
}
