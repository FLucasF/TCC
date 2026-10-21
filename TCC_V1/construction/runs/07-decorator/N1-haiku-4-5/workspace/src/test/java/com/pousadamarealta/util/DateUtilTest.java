package com.pousadamarealta.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class DateUtilTest {
    @Test
    public void testHighSeasonJuly() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    public void testHighSeasonDecemberAndJanuary() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2026, 12, 15)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2026, 12, 31)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 1, 31)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testLowSeason() {
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2026, 12, 14)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 6, 30)));
    }

    @Test
    public void testCountNights() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        assertEquals(2, DateUtil.countNights(checkIn, checkOut));
    }
}
