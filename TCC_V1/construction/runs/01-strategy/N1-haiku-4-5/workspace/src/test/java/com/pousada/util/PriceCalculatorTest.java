package com.pousada.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PriceCalculatorTest {
    @Test
    void testHighSeasonDecember() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    void testHighSeasonJanuary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 31)));
    }

    @Test
    void testHighSeasonFebruary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void testHighSeasonJuly() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testLowSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 11, 15)));
    }

    @Test
    void testCalculateNights() {
        assertEquals(2, PriceCalculator.calculateNights(
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12)));
    }

    @Test
    void testWeekendPackageValidation() {
        assertTrue(PriceCalculator.isWeekendPackageValid(
            LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14)));
        assertFalse(PriceCalculator.isWeekendPackageValid(
            LocalDate.of(2027, 3, 13), LocalDate.of(2027, 3, 15)));
    }
}
