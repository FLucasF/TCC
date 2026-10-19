package pousada.domain.document;

import pousada.domain.Room.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Os dados da estadia paga que qualquer documento mostra. */
public record Voucher(
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

    public boolean hasDiscount() {
        return discount.signum() > 0;
    }
}
