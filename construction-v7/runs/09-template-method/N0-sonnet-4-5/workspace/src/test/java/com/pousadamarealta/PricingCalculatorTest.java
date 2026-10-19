package com.pousadamarealta;

import com.pousadamarealta.util.PricingCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingCalculatorTest {

    @Test
    void testHighSeasonJuly() {
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testHighSeasonDecemberToFebruary() {
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2028, 1, 15)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2028, 2, 28)));
    }

    @Test
    void testLowSeason() {
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    void testNightPriceHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);
        BigDecimal expected = new BigDecimal("299.88");

        assertEquals(expected, PricingCalculator.calculateNightPrice(baseRate, highSeasonDate));
    }

    @Test
    void testSubtotalHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal expected = new BigDecimal("599.76");

        assertEquals(expected, PricingCalculator.calculateSubtotal(baseRate, checkIn, checkOut));
    }

    @Test
    void testRoundingHalfEven() {
        assertEquals(new BigDecimal("1.12"), PricingCalculator.roundToCents(new BigDecimal("1.125")));
        assertEquals(new BigDecimal("1.14"), PricingCalculator.roundToCents(new BigDecimal("1.135")));
    }
}
