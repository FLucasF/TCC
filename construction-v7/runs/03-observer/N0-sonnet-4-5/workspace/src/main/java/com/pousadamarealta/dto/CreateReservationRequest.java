package com.pousadamarealta.dto;

import com.pousadamarealta.domain.RatePlan;

public record CreateReservationRequest(
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
    Integer guests,
    RatePlan ratePlan
) {
}
