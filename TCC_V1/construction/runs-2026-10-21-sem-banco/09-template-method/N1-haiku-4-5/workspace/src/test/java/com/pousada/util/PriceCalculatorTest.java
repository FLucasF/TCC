package com.pousada.util;

import org.junit.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class PriceCalculatorTest {
    @Test
    public void testHighSeasonJuly() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    public void testHighSeasonJanuary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 10)));
    }

    @Test
    public void testHighSeasonFebruary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 10)));
    }

    @Test
    public void testLowSeasonMarch() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 10)));
    }

    @Test
    public void testNightlyRateCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal lowSeasonRate = PriceCalculator.calculateNightlyRate(baseRate, LocalDate.of(2027, 3, 10));
        assertEquals(new BigDecimal("249.90"), lowSeasonRate);

        BigDecimal highSeasonRate = PriceCalculator.calculateNightlyRate(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), highSeasonRate);
    }

    @Test
    public void testNightsCalculation() {
        int nights = PriceCalculator.calculateNights(LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(2, nights);
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(baseRate, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testDiscountCalculation() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    public void testNoDiscountStandard() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, "STANDARD");
        assertEquals(new BigDecimal("0.00"), discount);
    }

    @Test
    public void testRounding() {
        BigDecimal value = new BigDecimal("779.8");
        BigDecimal rounded = PriceCalculator.round(value);
        assertEquals(new BigDecimal("779.80"), rounded);
    }
}
