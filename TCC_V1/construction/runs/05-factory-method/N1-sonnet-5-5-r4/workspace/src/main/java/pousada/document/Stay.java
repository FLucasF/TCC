package pousada.document;

import pousada.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados da hospedagem que aparecem em qualquer documento. */
public record Stay(
        long reservationId,
        String guestName,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {
}
