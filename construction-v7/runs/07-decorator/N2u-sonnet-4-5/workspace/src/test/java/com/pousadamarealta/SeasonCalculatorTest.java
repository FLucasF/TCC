package com.pousadamarealta;

import com.pousadamarealta.domain.SeasonCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SeasonCalculatorTest {

    @Test
    void testHighSeasonJuly() {
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testHighSeasonDecember() {
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));
    }

    @Test
    void testHighSeasonJanuaryFebruary() {
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 1, 31)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 2, 1)));
        assertTrue(SeasonCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void testLowSeason() {
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 8, 20)));
        assertFalse(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 10)));
    }
}
