package com.pousadamarealta.dto;

import com.pousadamarealta.domain.RatePlan;

public record CreateReservationRequest(
    int roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
    int guests,
    RatePlan ratePlan
) {}
