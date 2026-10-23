package com.pousadamarealta.util;

import com.pousadamarealta.dto.CreateReservationDTO;

import java.time.LocalDate;

public class RequestValidator {

    public static void validateCreateReservation(CreateReservationDTO dto) {
        if (dto.roomNumber() == null || dto.roomNumber().isEmpty()) {
            throw new IllegalArgumentException("roomNumber is required");
        }
        if (dto.guestName() == null || dto.guestName().isEmpty()) {
            throw new IllegalArgumentException("guestName is required");
        }
        if (dto.guestEmail() == null || dto.guestEmail().isEmpty()) {
            throw new IllegalArgumentException("guestEmail is required");
        }
        if (dto.guestCpf() == null || dto.guestCpf().isEmpty()) {
            throw new IllegalArgumentException("guestCpf is required");
        }
        if (dto.checkIn() == null || dto.checkIn().isEmpty()) {
            throw new IllegalArgumentException("checkIn is required");
        }
        if (dto.checkOut() == null || dto.checkOut().isEmpty()) {
            throw new IllegalArgumentException("checkOut is required");
        }
        if (dto.ratePlan() == null || dto.ratePlan().isEmpty()) {
            throw new IllegalArgumentException("ratePlan is required");
        }

        LocalDate checkIn = LocalDate.parse(dto.checkIn());
        LocalDate checkOut = LocalDate.parse(dto.checkOut());

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }

        if (dto.guests() < 1) {
            throw new IllegalArgumentException("guests must be at least 1");
        }
    }
}
