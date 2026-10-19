package com.pousadamarealta.api.mapper;

import com.pousadamarealta.api.dto.DocumentDTO;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

public class DocumentMapper {
    public static DocumentDTO toDTO(Reservation reservation, Room room) {
        return new DocumentDTO(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType().toString(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }
}
