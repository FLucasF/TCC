package com.marealta.api.dto;

import com.marealta.domain.model.RatePlan;

import java.time.LocalDate;

public record CreateReservationRequest(
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    RatePlan ratePlan
) {
}
