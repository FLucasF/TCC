package com.pousadamarealta.api.dto;

public record ReservationRequest(
    int roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
    int guests,
    String ratePlan
) {
}
