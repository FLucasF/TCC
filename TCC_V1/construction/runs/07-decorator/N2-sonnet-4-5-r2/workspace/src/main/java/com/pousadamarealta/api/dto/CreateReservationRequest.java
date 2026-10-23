package com.pousadamarealta.api.dto;

public record CreateReservationRequest(
    int roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
    int guests,
    String ratePlan
) {}
