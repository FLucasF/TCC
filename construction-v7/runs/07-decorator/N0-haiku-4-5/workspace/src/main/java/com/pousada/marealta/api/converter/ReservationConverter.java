package com.pousada.marealta.api.converter;

import com.pousada.marealta.api.dto.ReservationDto;
import com.pousada.marealta.model.Reservation;

public class ReservationConverter {
    public static ReservationDto toDto(Reservation reservation) {
        return new ReservationDto(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlan().toString(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            reservation.getStatus().toString(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }
}
