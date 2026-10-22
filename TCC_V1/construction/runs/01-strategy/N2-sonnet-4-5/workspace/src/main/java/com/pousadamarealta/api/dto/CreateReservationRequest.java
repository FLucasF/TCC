package com.pousadamarealta.api.dto;

import java.time.LocalDate;

public record CreateReservationRequest(
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    String ratePlan,
    String companyCode
) {
}
