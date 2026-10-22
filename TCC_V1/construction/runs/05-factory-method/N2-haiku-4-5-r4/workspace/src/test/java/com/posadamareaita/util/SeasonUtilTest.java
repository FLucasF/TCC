package com.posadamareaita.util;

import java.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SeasonUtilTest {
    @Test
    public void testJulyIsHighSeason() {
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    public void testDecember15IsHighSeason() {
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 12, 31)));
    }

    @Test
    public void testJanuaryIsHighSeason() {
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 1, 31)));
    }

    @Test
    public void testFebruaryUntil28IsHighSeason() {
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 2, 1)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 2, 28)));
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2027, 3, 1)));
    }

    @Test
    public void testOtherMonthsAreNotHighSeason() {
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2027, 4, 10)));
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2027, 5, 10)));
    }
}
