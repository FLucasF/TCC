package com.pousada.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PriceCalculatorTest {

    @Test
    void testHighSeasonJuly() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testHighSeasonDecemberJanuary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2026, 12, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 15)));
    }

    @Test
    void testHighSeasonFebruary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 1)));
    }

    @Test
    void testLowSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 15)));
    }

    @Test
    void testNightlyPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");

        // Low season
        BigDecimal lowSeasonPrice = PriceCalculator.calculateNightlyPrice(
            baseRate, LocalDate.of(2027, 3, 15)
        );
        assertEquals(new BigDecimal("249.90"), lowSeasonPrice);

        // High season (20% more)
        BigDecimal highSeasonPrice = PriceCalculator.calculateNightlyPrice(
            baseRate, LocalDate.of(2027, 7, 15)
        );
        assertEquals(new BigDecimal("299.88"), highSeasonPrice);
    }

    @Test
    void testNights() {
        int nights = PriceCalculator.calculateNights(
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );
        assertEquals(2, nights);
    }

    @Test
    void testDiscountNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PriceCalculator.calculateDiscount(
            subtotal,
            com.pousada.domain.RatePlan.NON_REFUNDABLE
        );
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    void testIss() {
        BigDecimal total = new BigDecimal("779.80");
        BigDecimal iss = PriceCalculator.calculateIss(total);
        assertEquals(new BigDecimal("38.99"), iss);
    }
}
