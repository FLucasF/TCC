package br.com.marealta.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(
        long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
        LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, String companyCode,
        int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
        ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {}
