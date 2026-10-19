package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.ReservationDocument;
import java.math.BigDecimal;

public record DocumentResponse(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    int roomNumber,
    String roomType,
    String checkIn,
    String checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    public static DocumentResponse from(ReservationDocument doc) {
        return new DocumentResponse(
            doc.title(),
            doc.reservationId(),
            doc.guestName(),
            doc.guestCpf(),
            doc.roomNumber(),
            doc.roomType(),
            doc.checkIn().toString(),
            doc.checkOut().toString(),
            doc.nights(),
            doc.subtotal(),
            doc.discount(),
            doc.total()
        );
    }
}
