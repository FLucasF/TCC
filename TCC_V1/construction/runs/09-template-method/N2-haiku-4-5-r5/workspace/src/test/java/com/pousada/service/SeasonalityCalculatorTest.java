package com.pousada.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SeasonalityCalculatorTest {
    @Test
    void testJulyIsHighSeason() {
        assertTrue(SeasonalityCalculator.isHighSeason(LocalDate.of(2027, 7, 15)));
    }

    @Test
    void testDecember15IsHighSeason() {
        assertTrue(SeasonalityCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertFalse(SeasonalityCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    void testJanuaryIsHighSeason() {
        assertTrue(SeasonalityCalculator.isHighSeason(LocalDate.of(2027, 1, 15)));
    }

    @Test
    void testFebruaryIsHighSeason() {
        assertTrue(SeasonalityCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void testMarchIsNormalSeason() {
        assertFalse(SeasonalityCalculator.isHighSeason(LocalDate.of(2027, 3, 15)));
    }

    @Test
    void testAprilIsNormalSeason() {
        assertFalse(SeasonalityCalculator.isHighSeason(LocalDate.of(2027, 4, 15)));
    }

    @Test
    void testJuneIsNormalSeason() {
        assertFalse(SeasonalityCalculator.isHighSeason(LocalDate.of(2027, 6, 15)));
    }

    @Test
    void testAugustIsNormalSeason() {
        assertFalse(SeasonalityCalculator.isHighSeason(LocalDate.of(2027, 8, 15)));
    }

    @Test
    void testNovemberIsNormalSeason() {
        assertFalse(SeasonalityCalculator.isHighSeason(LocalDate.of(2027, 11, 15)));
    }
}
