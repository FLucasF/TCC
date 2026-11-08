package com.pousada;

import com.pousada.util.MoneyRounder;
import com.pousada.util.SeasonUtil;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class ReservationTest {

    @Test
    public void testHighSeasonDetection() {
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 2, 28)));
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2027, 12, 10)));
    }

    @Test
    public void testMoneyRounding() {
        BigDecimal value = new BigDecimal("249.90").multiply(new BigDecimal("1.20"));
        BigDecimal rounded = MoneyRounder.round(value);
        assertEquals("299.88", rounded.toString());
    }

}
