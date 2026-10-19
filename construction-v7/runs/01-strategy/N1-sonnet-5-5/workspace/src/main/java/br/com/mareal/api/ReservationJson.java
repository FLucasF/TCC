package br.com.mareal.api;

import br.com.mareal.domain.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;

/** JSON shape of a reservation. */
public record ReservationJson(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                              BigDecimal amountPaid, BigDecimal refund) {
    public static ReservationJson of(Reservation r) {
        return new ReservationJson(r.id, r.room.number(), r.guestName, r.guestEmail, r.guestCpf, r.checkIn,
                r.checkOut, r.guests, r.ratePlan.code(), r.nights, r.subtotal, r.discount, r.total,
                r.status.name(), r.amountPaid, r.refund);
    }
}
