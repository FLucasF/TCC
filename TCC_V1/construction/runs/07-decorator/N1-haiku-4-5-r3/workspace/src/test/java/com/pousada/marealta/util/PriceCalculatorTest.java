package com.pousada.marealta.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceCalculatorTest {
    @Test
    public void testHighSeasonJuly() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 20)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    public void testHighSeasonJanuary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 15)));
    }

    @Test
    public void testLowSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 4, 10)));
    }

    @Test
    public void testNightlyPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal lowSeasonPrice = PriceCalculator.calculateNightlyPrice(baseRate, LocalDate.of(2027, 4, 10));
        assertEquals(new BigDecimal("249.90"), lowSeasonPrice);

        BigDecimal highSeasonPrice = PriceCalculator.calculateNightlyPrice(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), highSeasonPrice);
    }

    @Test
    public void testRounding() {
        BigDecimal half = new BigDecimal("299.88");
        BigDecimal rounded = PriceCalculator.roundPrice(half);
        assertEquals(new BigDecimal("299.88"), rounded);

        BigDecimal halfUp = new BigDecimal("299.885");
        BigDecimal roundedUp = PriceCalculator.roundPrice(halfUp);
        assertEquals(new BigDecimal("299.88"), roundedUp);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(new BigDecimal("89.96"), discount);
    }
}
