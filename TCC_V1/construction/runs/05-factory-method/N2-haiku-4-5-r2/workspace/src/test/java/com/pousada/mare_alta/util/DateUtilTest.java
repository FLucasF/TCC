package com.pousada.mare_alta.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class DateUtilTest {
    @Test
    public void testHighSeasonJuly() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    public void testHighSeasonFebruary() {
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testNormalSeason() {
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2027, 6, 15)));
    }

    @Test
    public void testSeasonMultiplier() {
        BigDecimal highSeason = DateUtil.getSeasonMultiplier(LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("1.20"), highSeason);

        BigDecimal normalSeason = DateUtil.getSeasonMultiplier(LocalDate.of(2027, 3, 10));
        assertEquals(new BigDecimal("1.00"), normalSeason);
    }
}
