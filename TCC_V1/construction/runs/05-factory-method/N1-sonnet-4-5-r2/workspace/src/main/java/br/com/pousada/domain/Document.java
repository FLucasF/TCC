package br.com.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Document(
    String title,
    Long reservationId,
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
}
