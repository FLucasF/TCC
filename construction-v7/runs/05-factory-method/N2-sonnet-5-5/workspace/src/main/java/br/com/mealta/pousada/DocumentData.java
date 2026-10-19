package br.com.mealta.pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados da reserva que qualquer documento precisa mostrar. */
public record DocumentData(long reservationId, String guestName, int roomNumber, RoomType roomType,
                           LocalDate checkIn, LocalDate checkOut, int nights,
                           BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    public boolean hasDiscount() {
        return discount.signum() > 0;
    }
}
