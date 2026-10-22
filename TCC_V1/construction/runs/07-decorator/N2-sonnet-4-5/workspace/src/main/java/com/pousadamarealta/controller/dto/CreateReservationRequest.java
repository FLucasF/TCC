package com.pousadamarealta.controller.dto;

public record CreateReservationRequest(
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
    int guests,
    String ratePlan
) {}
