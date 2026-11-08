package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentDto(
    String title,
    Long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    RoomType roomType,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate checkIn,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    public static DocumentDto from(Document doc) {
        return new DocumentDto(
            doc.title(),
            doc.reservationId(),
            doc.guestName(),
            doc.guestCpf(),
            doc.roomNumber(),
            doc.roomType(),
            doc.checkIn(),
            doc.checkOut(),
            doc.nights(),
            doc.subtotal(),
            doc.discount(),
            doc.total()
        );
    }
}
