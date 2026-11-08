package br.com.marealta.service;

import br.com.marealta.domain.Document;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;

public class DocumentService {
    public Document generateDocument(Reservation reservation, Room room) {
        return new Document(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                room.getNumber(),
                room.getType(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
        );
    }
}
