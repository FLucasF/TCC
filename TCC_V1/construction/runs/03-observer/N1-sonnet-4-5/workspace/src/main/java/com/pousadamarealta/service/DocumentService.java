package com.pousadamarealta.service;

import com.pousadamarealta.domain.enums.RoomType;
import com.pousadamarealta.domain.model.Document;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.repository.RoomRepository;

public class DocumentService {
    private final RoomRepository roomRepository;

    public DocumentService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Document generateDocument(Reservation reservation) {
        if (!reservation.isPaid()) {
            throw new IllegalArgumentException("Document can only be generated for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
                .orElseThrow();
        RoomType roomType = room.getType();

        return new Document(
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
}
