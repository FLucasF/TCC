package br.com.pousadamarealta.api.dto;

import br.com.pousadamarealta.domain.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationDto(
    Long id,
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    String ratePlan,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String status,
    BigDecimal amountPaid,
    BigDecimal refund
) {
    public static ReservationDto from(Reservation reservation) {
        return new ReservationDto(
            reservation.id(),
            reservation.roomNumber(),
            reservation.guestName(),
            reservation.guestEmail(),
            reservation.guestCpf(),
            reservation.checkIn(),
            reservation.checkOut(),
            reservation.guests(),
            reservation.ratePlan().name(),
            reservation.nights(),
            reservation.subtotal(),
            reservation.discount(),
            reservation.total(),
            reservation.status().name(),
            reservation.amountPaid(),
            reservation.refund()
        );
    }
}
