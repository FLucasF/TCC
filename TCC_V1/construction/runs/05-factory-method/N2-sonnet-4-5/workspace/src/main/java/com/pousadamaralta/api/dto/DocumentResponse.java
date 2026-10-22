package com.pousadamaralta.api.dto;

import com.pousadamaralta.domain.identification.GuestIdentification;
import com.pousadamaralta.domain.model.Reservation;
import com.pousadamaralta.domain.model.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentResponse(
    String title,
    Long reservationId,
    String guestName,
    String guestCpf,
    String companyCnpj,
    String companyName,
    String guestPassport,
    String guestCountry,
    String roomNumber,
    String roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String text
) {
    public static DocumentResponse from(Reservation reservation, Room room, String documentText) {
        GuestIdentification id = reservation.getIdentification();

        String title = documentText.split("\n")[0].replaceAll(" nº \\d+$", "");

        return new DocumentResponse(
            title,
            reservation.getId(),
            reservation.getGuestName(),
            id.getCpf(),
            id.getCnpj(),
            id.getCompanyName(),
            id.getPassport(),
            id.getCountry(),
            reservation.getRoomNumber(),
            room.getType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            documentText
        );
    }
}
