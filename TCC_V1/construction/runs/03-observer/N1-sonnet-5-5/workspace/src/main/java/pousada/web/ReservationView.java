package pousada.web;

import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationView(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                              ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
    static ReservationView of(Reservation r) {
        return new ReservationView(r.id(), r.room().number(), r.guest().name(), r.guest().email(),
                r.guest().cpf(), r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.quote().nights(),
                r.quote().subtotal(), r.quote().discount(), r.quote().total(), r.status(),
                r.amountPaid(), r.refund());
    }
}
