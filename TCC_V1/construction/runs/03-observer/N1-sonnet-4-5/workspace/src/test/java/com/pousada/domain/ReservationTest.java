package com.pousada.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {
    @Test
    void shouldCalculateNights() {
        Reservation reservation = new Reservation(
            1, "101", "João", "joao@example.com", "12345678900",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
    }

    @Test
    void shouldCalculateSubtotalInHighSeason() {
        Reservation reservation = new Reservation(
            1, "101", "João", "joao@example.com", "12345678900",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        Money nightlyRate = new Money(new BigDecimal("249.90"));
        Money subtotal = reservation.calculateSubtotal(nightlyRate);

        assertEquals("599.76", subtotal.getAmount().toString());
    }

    @Test
    void shouldCalculateSubtotalInLowSeason() {
        Reservation reservation = new Reservation(
            1, "101", "João", "joao@example.com", "12345678900",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        Money nightlyRate = new Money(new BigDecimal("389.90"));
        Money subtotal = reservation.calculateSubtotal(nightlyRate);

        assertEquals("779.80", subtotal.getAmount().toString());
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        Reservation reservation = new Reservation(
            1, "101", "João", "joao@example.com", "12345678900",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        Money subtotal = new Money(new BigDecimal("779.80"));
        Money discount = reservation.calculateDiscount(subtotal);

        assertEquals("116.97", discount.getAmount().toString());
    }

    @Test
    void shouldNotRefundNonRefundablePlan() {
        Reservation reservation = new Reservation(
            1, "101", "João", "joao@example.com", "12345678900",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservation.confirm(new Money(new BigDecimal("662.83")));
        Money refund = reservation.cancel();

        assertEquals("0.00", refund.getAmount().toString());
    }

    @Test
    void shouldRefundStandardPlan() {
        Reservation reservation = new Reservation(
            1, "101", "João", "joao@example.com", "12345678900",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.confirm(new Money(new BigDecimal("779.80")));
        Money refund = reservation.cancel();

        assertEquals("779.80", refund.getAmount().toString());
    }

    @Test
    void shouldIdentifyHighSeasonInDecember() {
        Reservation reservation = new Reservation(
            1, "101", "João", "joao@example.com", "12345678900",
            LocalDate.of(2027, 12, 15),
            LocalDate.of(2027, 12, 16),
            2, RatePlan.STANDARD
        );

        Money nightlyRate = new Money(new BigDecimal("249.90"));
        Money subtotal = reservation.calculateSubtotal(nightlyRate);

        assertEquals("299.88", subtotal.getAmount().toString());
    }

    @Test
    void shouldIdentifyLowSeasonBeforeDecember15() {
        Reservation reservation = new Reservation(
            1, "101", "João", "joao@example.com", "12345678900",
            LocalDate.of(2027, 12, 14),
            LocalDate.of(2027, 12, 15),
            2, RatePlan.STANDARD
        );

        Money nightlyRate = new Money(new BigDecimal("249.90"));
        Money subtotal = reservation.calculateSubtotal(nightlyRate);

        assertEquals("249.90", subtotal.getAmount().toString());
    }
}
