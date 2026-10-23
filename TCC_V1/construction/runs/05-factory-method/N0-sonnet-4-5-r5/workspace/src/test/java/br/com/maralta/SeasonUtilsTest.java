package br.com.maralta;

import br.com.maralta.util.SeasonUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class SeasonUtilsTest {

    @Test
    void testJulyIsHighSeason() {
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testDecemberFrom15IsHighSeason() {
        assertFalse(SeasonUtils.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 12, 31)));
    }

    @Test
    void testJanuaryIsHighSeason() {
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 1, 31)));
    }

    @Test
    void testFebruaryIsHighSeason() {
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 2, 1)));
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 2, 15)));
        assertTrue(SeasonUtils.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void testOtherMonthsAreLowSeason() {
        assertFalse(SeasonUtils.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(SeasonUtils.isHighSeason(LocalDate.of(2027, 4, 15)));
        assertFalse(SeasonUtils.isHighSeason(LocalDate.of(2027, 5, 15)));
        assertFalse(SeasonUtils.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertFalse(SeasonUtils.isHighSeason(LocalDate.of(2027, 8, 15)));
        assertFalse(SeasonUtils.isHighSeason(LocalDate.of(2027, 9, 15)));
        assertFalse(SeasonUtils.isHighSeason(LocalDate.of(2027, 10, 15)));
        assertFalse(SeasonUtils.isHighSeason(LocalDate.of(2027, 11, 15)));
    }
}
