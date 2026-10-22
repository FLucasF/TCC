package com.pousada.marealta.util;

import junit.framework.TestCase;
import java.time.LocalDate;

public class SeasonCalculatorTest extends TestCase {
    public void testHighSeason() {
        // July is high season
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 10)));

        // December 15+ is high season
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));

        // January-February is high season
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    public void testLowSeason() {
        // December 1-14 is low season
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 1)));
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));

        // March-June is low season
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 6, 30)));

        // August-November is low season
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 8, 1)));
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 11, 30)));
    }
}
