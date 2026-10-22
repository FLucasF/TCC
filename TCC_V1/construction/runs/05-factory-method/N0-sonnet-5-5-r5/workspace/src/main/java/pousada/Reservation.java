package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(
        long id,
        int roomNumber,
        String guestName,
        String guestEmail,
        Identification identification,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        Status status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    public RoomType roomType() {
        return Room.find(roomNumber).orElseThrow().type();
    }

    /** Dinheiro que ficou com a pousada: pago menos devolvido. */
    public BigDecimal kept() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }
}
