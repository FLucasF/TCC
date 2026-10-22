package pousada.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import pousada.domain.RoomType;

/** O que todo documento mostra da reserva, qualquer que seja o tipo. */
public record DocumentData(
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
