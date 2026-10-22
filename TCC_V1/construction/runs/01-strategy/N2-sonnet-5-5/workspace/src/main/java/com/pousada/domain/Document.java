package com.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Document(
        String title,
        long reservationId,
        String guestName,
        String guestCpf,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {

    public static Document of(Reservation r, Room room) {
        return new Document("Comprovante de reserva", r.id(), r.guestName(), r.guestCpf(), room.number(),
                room.type(), r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }
}
