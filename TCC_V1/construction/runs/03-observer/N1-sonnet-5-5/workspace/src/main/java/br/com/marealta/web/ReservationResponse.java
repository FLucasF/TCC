package br.com.marealta.web;

import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                                  LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                  int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                                  ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {

    static ReservationResponse of(Reservation r) {
        return new ReservationResponse(r.id(), r.room().number(), r.guestName(), r.guestEmail(), r.guestCpf(),
                r.checkInDate(), r.checkOutDate(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(),
                r.discount(), r.total(), r.status(), r.amountPaid(), r.refund());
    }
}
