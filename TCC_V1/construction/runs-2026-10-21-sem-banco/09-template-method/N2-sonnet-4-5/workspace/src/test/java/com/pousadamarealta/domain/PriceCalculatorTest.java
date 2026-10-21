package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PriceCalculatorTest {

    @Test
    void testHighSeasonDecember() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 20)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    void testHighSeasonJanuaryFebruary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void testHighSeasonJuly() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testLowSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 30)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void testHighSeasonPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);
        BigDecimal expected = new BigDecimal("299.88");

        BigDecimal result = PriceCalculator.calculateNightPrice(baseRate, highSeasonDate);

        assertEquals(expected, result);
    }

    @Test
    void testLowSeasonPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate lowSeasonDate = LocalDate.of(2027, 3, 10);

        BigDecimal result = PriceCalculator.calculateNightPrice(baseRate, lowSeasonDate);

        assertEquals(baseRate.setScale(2), result);
    }
}
