package com.pousada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Document;
import com.pousada.domain.RoomType;

public record DocumentResponse(
    @JsonProperty("title") String title,
    @JsonProperty("reservationId") long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("companyCnpj") String companyCnpj,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("roomType") String roomType,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") String subtotal,
    @JsonProperty("discount") String discount,
    @JsonProperty("total") String total,
    @JsonProperty("text") String text
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
            document.getTitle(),
            document.getReservationId(),
            document.getGuestName(),
            document.getGuestCpf(),
            document.getCompanyName(),
            document.getCompanyCnpj(),
            document.getGuestPassport(),
            document.getGuestCountry(),
            document.getRoomNumber(),
            document.getRoomType().name(),
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
