package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void testHighSeasonJuly() {
        Reservation reservation = new Reservation(
            1, "101", "Test", "test@test.com",
            new CpfIdentification("111.222.333-44"),
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        BigDecimal nightlyRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.getSubtotal(nightlyRate);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        Reservation reservation = new Reservation(
            1, "101", "Test", "test@test.com",
            new CpfIdentification("111.222.333-44"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        BigDecimal nightlyRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.getSubtotal(nightlyRate);
        BigDecimal discount = reservation.getDiscount(subtotal);

        assertEquals(new BigDecimal("499.80"), subtotal);
        assertEquals(new BigDecimal("74.97"), discount);
    }

    @Test
    void testRefundOnCancellation() {
        Reservation reservation = new Reservation(
            1, "101", "Test", "test@test.com",
            new CpfIdentification("111.222.333-44"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.confirm(new BigDecimal("100.00"));
        reservation.cancel();

        assertEquals(new BigDecimal("100.00"), reservation.getRefund());
    }

    @Test
    void testNoRefundOnNonRefundableCancellation() {
        Reservation reservation = new Reservation(
            1, "101", "Test", "test@test.com",
            new CpfIdentification("111.222.333-44"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservation.confirm(new BigDecimal("100.00"));
        reservation.cancel();

        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }
}
