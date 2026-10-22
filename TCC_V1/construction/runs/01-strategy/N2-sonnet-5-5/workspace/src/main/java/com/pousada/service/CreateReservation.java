package com.pousada.service;

import java.time.LocalDate;

public record CreateReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String ratePlan,
        String companyCode) {
}
