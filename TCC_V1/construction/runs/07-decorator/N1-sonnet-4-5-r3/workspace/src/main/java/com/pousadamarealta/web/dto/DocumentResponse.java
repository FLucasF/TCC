package com.pousadamarealta.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.ReservationDocument;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentResponse(
    String title,
    @JsonProperty("reservationId") long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("roomType") String roomType,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    public static DocumentResponse from(ReservationDocument doc) {
        return new DocumentResponse(
            "Comprovante de reserva",
            doc.getReservationId(),
            doc.getGuestName(),
            doc.getGuestCpf(),
            doc.getRoomNumber(),
            doc.getRoomType().name(),
            doc.getCheckIn(),
            doc.getCheckOut(),
            doc.getNights(),
            doc.getSubtotal(),
            doc.getDiscount(),
            doc.getTotal()
        );
    }
}
