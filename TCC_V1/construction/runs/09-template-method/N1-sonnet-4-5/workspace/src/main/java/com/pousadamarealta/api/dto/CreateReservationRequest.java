package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.RatePlan;

import java.time.LocalDate;

public record CreateReservationRequest(
        String roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer guests,
        RatePlan ratePlan
) {
}
