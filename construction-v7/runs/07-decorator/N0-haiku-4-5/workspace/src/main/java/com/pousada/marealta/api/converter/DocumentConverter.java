package com.pousada.marealta.api.converter;

import com.pousada.marealta.api.dto.DocumentDto;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.ReservationDocument;
import com.pousada.marealta.service.RoomManager;

public class DocumentConverter {
    private static RoomManager roomManager;

    public DocumentConverter(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    public static ReservationDocument toDocument(Reservation reservation, RoomManager roomManager) {
        String roomType = roomManager.getRoom(reservation.getRoomNumber())
            .map(r -> r.type().toString())
            .orElse("UNKNOWN");

        return new ReservationDocument(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            roomType,
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    public static DocumentDto toDto(ReservationDocument document) {
        return new DocumentDto(
            document.title(),
            document.reservationId(),
            document.guestName(),
            document.guestCpf(),
            document.roomNumber(),
            document.roomType(),
            document.checkIn(),
            document.checkOut(),
            document.nights(),
            document.subtotal(),
            document.discount(),
            document.total()
        );
    }
}
