package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Reservation;
import java.time.LocalDate;

public record DocumentResponse(
    @JsonProperty("title") String title,
    @JsonProperty("reservationId") long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("roomNumber") int roomNumber,
    @JsonProperty("roomType") String roomType,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") double subtotal,
    @JsonProperty("discount") double discount,
    @JsonProperty("total") double total
) {
    public static DocumentResponse from(Reservation r) {
        return new DocumentResponse(
            "Comprovante de reserva",
            r.id,
            r.guestName,
            formatCpf(r.guestCpf),
            r.roomNumber,
            getRoomType(r.roomNumber),
            formatDateBr(r.checkIn),
            formatDateBr(r.checkOut),
            r.nights,
            r.subtotal,
            r.discount,
            r.total
        );
    }

    private static String formatDateBr(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private static String getRoomType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }
}
