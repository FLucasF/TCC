package com.pousadamareaita.pricing;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.math.BigDecimal;

public class PricingUtilsTest {
    @Test
    public void testHighSeasonJuly() {
        assertTrue(PricingUtils.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertFalse(PricingUtils.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(PricingUtils.isHighSeason(LocalDate.of(2027, 12, 15)));
    }

    @Test
    public void testHighSeasonJanuary() {
        assertTrue(PricingUtils.isHighSeason(LocalDate.of(2027, 1, 10)));
    }

    @Test
    public void testHighSeasonFebruary() {
        assertTrue(PricingUtils.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testLowSeasonMarch() {
        assertFalse(PricingUtils.isHighSeason(LocalDate.of(2027, 3, 10)));
    }

    @Test
    public void testNightlyRateHighSeason() {
        BigDecimal baseRate = BigDecimal.valueOf(249.90);
        BigDecimal highSeasonRate = PricingUtils.getNightlyRate(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(0, highSeasonRate.compareTo(new BigDecimal("299.88")));
    }

    @Test
    public void testNightlyRateLowSeason() {
        BigDecimal baseRate = BigDecimal.valueOf(249.90);
        BigDecimal lowSeasonRate = PricingUtils.getNightlyRate(baseRate, LocalDate.of(2027, 3, 10));
        assertEquals(baseRate, lowSeasonRate);
    }

    @Test
    public void testRoundBanker() {
        assertEquals(0, new BigDecimal("93.58").compareTo(PricingUtils.roundBanker(BigDecimal.valueOf(93.576))));
        assertEquals(0, new BigDecimal("93.56").compareTo(PricingUtils.roundBanker(BigDecimal.valueOf(93.565))));
    }

    @Test
    public void testCountNights() {
        assertEquals(2, PricingUtils.countNights(LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12)));
        assertEquals(7, PricingUtils.countNights(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8)));
    }
}
