package br.com.pousada.api;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record CreateReservationRequest(
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String companyCnpj,
    String companyName,
    String guestPassport,
    String guestCountry,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate checkIn,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate checkOut,
    Integer guests,
    String ratePlan
) {
}
