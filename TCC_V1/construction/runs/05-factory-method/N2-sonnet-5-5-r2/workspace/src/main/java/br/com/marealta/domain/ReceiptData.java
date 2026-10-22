package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados da reserva que qualquer documento mostra. */
public record ReceiptData(long reservationId, String guestName, int roomNumber, RoomType roomType,
                          LocalDate checkIn, LocalDate checkOut, int nights,
                          BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
}
