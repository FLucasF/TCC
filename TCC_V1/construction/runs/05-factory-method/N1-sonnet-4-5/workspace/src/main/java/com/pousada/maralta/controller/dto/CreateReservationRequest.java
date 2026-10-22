package com.pousada.maralta.controller.dto;

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
    LocalDate checkIn,
    LocalDate checkOut,
    Integer guests,
    String ratePlan
) {
}
