package com.pousadamarealta.api.mapper;

import com.pousadamarealta.api.dto.ReservationDTO;
import com.pousadamarealta.domain.Reservation;

public class ReservationMapper {
    public static ReservationDTO toDTO(Reservation reservation) {
        return new ReservationDTO(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlanType().toString(),
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
