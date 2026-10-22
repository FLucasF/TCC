package br.com.marealta.web;

import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                                  LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, int nights,
                                  BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                                  BigDecimal amountPaid, BigDecimal refund) {

    public static ReservationResponse of(Reservation r) {
        return new ReservationResponse(r.id(), r.roomNumber(), r.guest().name(), r.guest().email(),
                r.guest().cpf(), r.arrival(), r.departure(), r.guests(), r.ratePlan().name(),
                r.quote().nights(), r.quote().subtotal(), r.quote().discount(), r.quote().total(),
                r.status().name(), r.amountPaid(), r.refund());
    }
}
