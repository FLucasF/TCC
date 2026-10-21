package com.pousadamarealta;

import static org.junit.jupiter.api.Assertions.*;

import com.pousadamarealta.util.DateUtil;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateUtilTest {
    @Test
    void testHighSeasonJuly() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    void testHighSeasonDecember() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    void testHighSeasonFebruary() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void testHighSeasonJanuary() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 1, 15)));
    }

    @Test
    void testLowSeason() {
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 4, 15)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 5, 15)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 8, 15)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 9, 15)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 10, 15)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 11, 15)));
    }

    @Test
    void testDaysBetween() {
        LocalDate start = LocalDate.of(2027, 7, 10);
        LocalDate end = LocalDate.of(2027, 7, 12);
        assertEquals(2, DateUtil.daysBetween(start, end));
    }
}
