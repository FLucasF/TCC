package com.pousadamareal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(
    long id,
    int roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String companyCnpj,
    String companyName,
    String guestPassport,
    String guestCountry,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    RatePlan ratePlan,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    ReservationStatus status,
    BigDecimal amountPaid,
    BigDecimal refund
) {
    public Reservation withStatus(ReservationStatus status) {
        return new Reservation(
            id, roomNumber, guestName, guestEmail, guestCpf, companyCnpj, companyName,
            guestPassport, guestCountry, checkIn, checkOut, guests, ratePlan, nights,
            subtotal, discount, total, status, amountPaid, refund
        );
    }

    public Reservation withAmountPaid(BigDecimal amountPaid) {
        return new Reservation(
            id, roomNumber, guestName, guestEmail, guestCpf, companyCnpj, companyName,
            guestPassport, guestCountry, checkIn, checkOut, guests, ratePlan, nights,
            subtotal, discount, total, status, amountPaid, refund
        );
    }

    public Reservation withRefund(BigDecimal refund) {
        return new Reservation(
            id, roomNumber, guestName, guestEmail, guestCpf, companyCnpj, companyName,
            guestPassport, guestCountry, checkIn, checkOut, guests, ratePlan, nights,
            subtotal, discount, total, status, amountPaid, refund
        );
    }
}
