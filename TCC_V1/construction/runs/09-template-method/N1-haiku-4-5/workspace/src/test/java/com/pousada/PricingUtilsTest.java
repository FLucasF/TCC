package com.pousada;

import com.pousada.utils.PricingUtils;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class PricingUtilsTest {

    @Test
    public void testHighSeasonJuly() {
        LocalDate date = LocalDate.of(2027, 7, 10);
        assertTrue(PricingUtils.isHighSeason(date));
    }

    @Test
    public void testHighSeasonDecember() {
        LocalDate date = LocalDate.of(2027, 12, 25);
        assertTrue(PricingUtils.isHighSeason(date));
    }

    @Test
    public void testLowSeason() {
        LocalDate date = LocalDate.of(2027, 5, 10);
        assertFalse(PricingUtils.isHighSeason(date));
    }

    @Test
    public void testNightlyPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);
        BigDecimal price = PricingUtils.calculateNightlyPrice(baseRate, highSeasonDate);
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    public void testRoundingToPair() {
        BigDecimal value = new BigDecimal("299.885");
        BigDecimal rounded = PricingUtils.roundToPair(value);
        assertEquals(new BigDecimal("299.88"), rounded);
    }
}
