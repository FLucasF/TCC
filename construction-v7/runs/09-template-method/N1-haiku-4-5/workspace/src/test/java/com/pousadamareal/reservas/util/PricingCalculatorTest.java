package com.pousadamareal.reservas.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PricingCalculatorTest {
    private static final BigDecimal STANDARD_RATE = new BigDecimal("249.90");
    private static final BigDecimal HIGH_SEASON_RATE = new BigDecimal("299.88");

    @Test
    public void testHighSeasonJuly() {
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    public void testHighSeasonJanuary() {
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 1, 15)));
    }

    @Test
    public void testHighSeasonFebruary() {
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 2, 15)));
    }

    @Test
    public void testLowSeasonOther() {
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 11, 15)));
    }

    @Test
    public void testNightlyPriceHighSeason() {
        BigDecimal price = PricingCalculator.calculateNightlyPrice(STANDARD_RATE, LocalDate.of(2027, 7, 10));
        assertEquals(HIGH_SEASON_RATE, price);
    }

    @Test
    public void testSubtotalTwoNightsHighSeason() {
        BigDecimal subtotal = PricingCalculator.calculateSubtotal(STANDARD_RATE,
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testDiscountNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, true);
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testDiscountStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, false);
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    public void testCalculateNights() {
        long nights = PricingCalculator.calculateNights(
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(2, nights);
    }
}
