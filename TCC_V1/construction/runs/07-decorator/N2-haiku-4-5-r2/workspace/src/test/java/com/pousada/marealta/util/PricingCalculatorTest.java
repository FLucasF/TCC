package com.pousada.marealta.util;

import com.pousada.marealta.domain.Reservation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingCalculatorTest {
    @Test
    void shouldCalculateHighSeasonMultiplier() {
        LocalDate julyDate = LocalDate.of(2027, 7, 10);
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal result = PricingCalculator.calculateNightlyRate(rate, julyDate);
        assertEquals(new BigDecimal("299.88"), result);
    }

    @Test
    void shouldNotApplyMultiplierInLowSeason() {
        LocalDate mayDate = LocalDate.of(2027, 5, 10);
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal result = PricingCalculator.calculateNightlyRate(rate, mayDate);
        assertEquals(rate, result);
    }

    @Test
    void shouldCalculateSubtotal() {
        BigDecimal rate = new BigDecimal("299.88");
        BigDecimal result = PricingCalculator.calculateSubtotal(rate, 2);
        assertEquals(new BigDecimal("599.76"), result);
    }

    @Test
    void shouldCalculateNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal result = PricingCalculator.calculateDiscount(subtotal, Reservation.RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), result);
    }

    @Test
    void shouldNotApplyStandardDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal result = PricingCalculator.calculateDiscount(subtotal, Reservation.RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal result = PricingCalculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), result);
    }
}
