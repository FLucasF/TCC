package com.pousada.util;

import org.junit.Test;
import java.time.LocalDate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class DateUtilTest {
    @Test
    public void testHighSeasonJuly() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 12, 15)));
    }

    @Test
    public void testHighSeasonJanuary() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 1, 15)));
    }

    @Test
    public void testHighSeasonFebruary() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 2, 15)));
    }

    @Test
    public void testLowSeasonMarch() {
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 3, 15)));
    }

    @Test
    public void testCountNights() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        assertEquals(2, DateUtil.countNights(checkIn, checkOut));
    }
}
