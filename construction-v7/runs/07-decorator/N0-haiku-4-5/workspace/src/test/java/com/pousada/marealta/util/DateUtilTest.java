package com.pousada.marealta.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {
    @Test
    void testJuly() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testDecemberHighSeason() {
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2026, 12, 14)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2026, 12, 15)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2026, 12, 31)));
    }

    @Test
    void testJanuaryAndFebruary() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void testOffSeason() {
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 6, 30)));
    }
}
