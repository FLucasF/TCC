package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentResponse(
    String title,
    @JsonProperty("reservationId") long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("companyCnpj") String companyCnpj,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("roomType") RoomType roomType,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String text
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
            document.getTitle(),
            document.getReservationId(),
            document.getGuestName(),
            document.getGuestCpf(),
            document.getCompanyCnpj(),
            document.getCompanyName(),
            document.getGuestPassport(),
            document.getGuestCountry(),
            document.getRoomNumber(),
            document.getRoomType(),
            document.getCheckIn(),
            document.getCheckOut(),
            document.getNights(),
            document.getSubtotal(),
            document.getDiscount(),
            document.getTotal(),
            document.getText()
        );
    }
}
