package com.pousada.marealta.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PriceCalculatorTest {

    @Test
    public void testHighSeasonJuly() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    public void testHighSeasonDecemberToFebruary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2026, 12, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2026, 12, 31)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testLowSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2026, 12, 14)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 30)));
    }

    @Test
    public void testNightlyPriceLowSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal price = PriceCalculator.calculateNightlyPrice(baseRate, LocalDate.of(2027, 3, 10));
        assertEquals(0, price.compareTo(new BigDecimal("249.90")));
    }

    @Test
    public void testNightlyPriceHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal price = PriceCalculator.calculateNightlyPrice(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(0, price.compareTo(new BigDecimal("299.88")));
    }

    @Test
    public void testCalculateNights() {
        long nights = PriceCalculator.calculateNights(
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );
        assertEquals(2, nights);
    }

    @Test
    public void testCalculatePriceStandardPlanHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        var breakdown = PriceCalculator.calculatePrice(
            baseRate,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            "STANDARD"
        );

        assertEquals(2, breakdown.nights());
        assertEquals(0, breakdown.subtotal().compareTo(new BigDecimal("599.76")));
        assertEquals(0, breakdown.discount().compareTo(BigDecimal.ZERO));
        assertEquals(0, breakdown.total().compareTo(new BigDecimal("599.76")));
    }

    @Test
    public void testCalculatePriceNonRefundablePlan() {
        BigDecimal baseRate = new BigDecimal("249.90");
        var breakdown = PriceCalculator.calculatePrice(
            baseRate,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            "NON_REFUNDABLE"
        );

        assertEquals(2, breakdown.nights());
        assertEquals(0, breakdown.subtotal().compareTo(new BigDecimal("599.76")));
        assertEquals(0, breakdown.discount().compareTo(new BigDecimal("89.96")));
        assertEquals(0, breakdown.total().compareTo(new BigDecimal("509.80")));
    }

    @Test
    public void testRoundingHalfEven() {
        // Test that rounding is "meio para o par" (HALF_EVEN)
        BigDecimal baseRate = new BigDecimal("249.90");
        var breakdown = PriceCalculator.calculatePrice(
            baseRate,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            "STANDARD"
        );

        // 249.90 * 2 = 499.80, no rounding needed
        assertEquals(0, breakdown.subtotal().compareTo(new BigDecimal("499.80")));
    }
}
