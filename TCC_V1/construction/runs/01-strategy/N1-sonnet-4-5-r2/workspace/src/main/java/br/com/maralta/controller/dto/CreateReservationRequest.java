package br.com.maralta.controller.dto;

import java.time.LocalDate;

public record CreateReservationRequest(
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    LocalDate checkIn,
    LocalDate checkOut,
    Integer guests,
    String ratePlan,
    String companyCode
) {
}
