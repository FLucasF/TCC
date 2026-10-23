package com.pousadamarealta.dto;

public record CreateReservationDTO(
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
    int guests,
    String ratePlan
) {
}
