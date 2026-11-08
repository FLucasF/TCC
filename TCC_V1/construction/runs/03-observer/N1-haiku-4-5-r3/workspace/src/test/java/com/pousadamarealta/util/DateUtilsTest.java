package com.pousadamarealta.util;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class DateUtilsTest {
    @Test
    public void testHighSeasonJuly() {
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 12, 31)));
    }

    @Test
    public void testHighSeasonJanuaryFebruary() {
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testCountNights() {
        int nights = DateUtils.countNights(
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12)
        );
        assertEquals(2, nights);
    }

    @Test
    public void testFormatDate() {
        String formatted = DateUtils.formatDate(LocalDate.of(2027, 3, 5));
        assertEquals("05/03/2027", formatted);
    }
}
