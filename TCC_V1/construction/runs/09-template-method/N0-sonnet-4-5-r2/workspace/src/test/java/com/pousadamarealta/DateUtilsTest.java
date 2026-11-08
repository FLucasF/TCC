package com.pousadamarealta;

import com.pousadamarealta.util.DateUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DateUtilsTest {

    @Test
    public void testJulyIsHighSeason() {
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    public void testDecember15OnwardsIsHighSeason() {
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 12, 31)));
    }

    @Test
    public void testJanuaryIsHighSeason() {
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 1, 31)));
    }

    @Test
    public void testFebruaryIsHighSeason() {
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 2, 1)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testMarchIsLowSeason() {
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 3, 15)));
    }
}
