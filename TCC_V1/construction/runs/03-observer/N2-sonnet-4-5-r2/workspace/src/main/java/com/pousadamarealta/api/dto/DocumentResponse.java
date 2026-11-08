package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.ReservationDocument;
import com.pousadamarealta.domain.RoomType;

public record DocumentResponse(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    RoomType roomType,
    String checkIn,
    String checkOut,
    int nights,
    double subtotal,
    double discount,
    double total
) {
    public static DocumentResponse from(ReservationDocument document) {
        return new DocumentResponse(
            document.getTitle(),
            document.getReservationId(),
            document.getGuestName(),
            document.getGuestCpf(),
            document.getRoomNumber(),
            document.getRoomType(),
            document.getCheckIn().toString(),
            document.getCheckOut().toString(),
            document.getNights(),
            document.getSubtotal().doubleValue(),
            document.getDiscount().doubleValue(),
            document.getTotal().doubleValue()
        );
    }
}
