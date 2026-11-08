package br.com.pousadamarealta.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DateUtilsTest {

    @Test
    public void testFormatDisplay() {
        LocalDate date = LocalDate.of(2027, 7, 10);
        assertEquals("10/07/2027", DateUtils.formatDisplay(date));
    }

    @Test
    public void testFormatIso() {
        LocalDate date = LocalDate.of(2027, 7, 10);
        assertEquals("2027-07-10", DateUtils.formatIso(date));
    }

    @Test
    public void testHighSeasonJuly() {
        // Julho inteiro é alta temporada
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    public void testHighSeasonDecemberToFebruary() {
        // 15 de dezembro até fim de fevereiro
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2028, 1, 15)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2028, 2, 28)));
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2028, 3, 1)));
    }

    @Test
    public void testLowSeason() {
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 6, 30)));
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 8, 1)));
    }
}
