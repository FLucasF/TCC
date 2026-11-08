package com.pousada.marealba.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DateUtilTest {

    @Test
    public void testIsHighSeasonJuly() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    public void testIsHighSeasonDecember() {
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 12, 31)));
    }

    @Test
    public void testIsHighSeasonJanuaryFebruary() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 1, 31)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 2, 1)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testIsNotHighSeason() {
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 6, 30)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    public void testFormatDisplay() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        assertEquals("10/03/2027", DateUtil.formatDisplay(date));
    }

    @Test
    public void testFormatIso() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        assertEquals("2027-03-10", DateUtil.formatIso(date));
    }

    @Test
    public void testParseIso() {
        LocalDate date = DateUtil.parseIso("2027-03-10");
        assertEquals(LocalDate.of(2027, 3, 10), date);
    }
}
