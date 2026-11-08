package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentResponse(
    @JsonProperty("title") String title,
    @JsonProperty("reservationId") Long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("roomType") String roomType,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") BigDecimal subtotal,
    @JsonProperty("discount") BigDecimal discount,
    @JsonProperty("total") BigDecimal total
) {
    public static DocumentResponse from(Reservation r) {
        return new DocumentResponse(
            "Comprovante de reserva",
            r.getId(),
            r.getGuestName(),
            r.getGuestCpf(),
            r.getRoomNumber(),
            getRoomTypeName(r.getRoomNumber()),
            r.getCheckIn(),
            r.getCheckOut(),
            r.getNights(),
            r.getSubtotal(),
            r.getDiscount(),
            r.getTotal()
        );
    }

    private static String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "UNKNOWN";
        };
    }
}
