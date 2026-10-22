package com.pousada.maralta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationDocument(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    String companyCnpj,
    String companyName,
    String guestPassport,
    String guestCountry,
    String roomNumber,
    RoomType roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String text
) {
}
