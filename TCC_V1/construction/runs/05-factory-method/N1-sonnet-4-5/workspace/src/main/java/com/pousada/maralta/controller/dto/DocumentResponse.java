package com.pousada.maralta.controller.dto;

import com.pousada.maralta.domain.ReservationDocument;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentResponse(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    String companyCnpj,
    String companyName,
    String guestPassport,
    String guestCountry,
    String roomNumber,
    String roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String text
) {
    public static DocumentResponse from(ReservationDocument doc) {
        return new DocumentResponse(
            doc.title(),
            doc.reservationId(),
            doc.guestName(),
            doc.guestCpf(),
            doc.companyCnpj(),
            doc.companyName(),
            doc.guestPassport(),
            doc.guestCountry(),
            doc.roomNumber(),
            doc.roomType().name(),
            doc.checkIn(),
            doc.checkOut(),
            doc.nights(),
            doc.subtotal(),
            doc.discount(),
            doc.total(),
            doc.text()
        );
    }
}
