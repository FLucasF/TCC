package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.DocumentGenerator;
import com.pousada.domain.GuestIdentification;
import com.pousada.domain.Reservation;

import java.math.BigDecimal;

public record DocumentDto(
    String title,
    @JsonProperty("reservationId") long reservationId,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("companyCnpj") String companyCnpj,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("roomType") String roomType,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String text
) {
    public static DocumentDto from(Reservation reservation) {
        GuestIdentification id = reservation.getIdentification();
        String guestCpf = null;
        String companyCnpj = null;
        String companyName = null;
        String guestPassport = null;
        String guestCountry = null;
        String title;

        if (id instanceof GuestIdentification.Cpf cpf) {
            guestCpf = cpf.cpf();
            title = "Comprovante de reserva";
        } else if (id instanceof GuestIdentification.Cnpj cnpj) {
            companyCnpj = cnpj.cnpj();
            companyName = cnpj.companyName();
            title = "Fatura de hospedagem";
        } else {
            GuestIdentification.Passport passport = (GuestIdentification.Passport) id;
            guestPassport = passport.passport();
            guestCountry = passport.country();
            title = "Booking receipt";
        }

        DocumentGenerator generator = DocumentGenerator.forIdentification(id);
        String text = generator.generate(reservation);

        return new DocumentDto(
            title,
            reservation.getId(),
            reservation.getGuestName(),
            guestCpf,
            companyCnpj,
            companyName,
            guestPassport,
            guestCountry,
            reservation.getRoomNumber(),
            getRoomType(reservation.getRoomNumber()),
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            text
        );
    }

    private static String getRoomType(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }
}
