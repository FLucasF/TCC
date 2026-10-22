package com.pousada;

import com.pousada.utils.DateUtil;
import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;

public class DateUtilTest {
    @Test
    public void highSeasonInJuly() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 1)));
    }

    @Test
    public void highSeasonInDecember() {
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2026, 12, 14)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2026, 12, 15)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2026, 12, 31)));
    }

    @Test
    public void highSeasonJanuaryToFebruary() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void lowSeason() {
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 6, 30)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    public void countNights() {
        int nights = DateUtil.countNights(LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(2, nights);
    }
}
