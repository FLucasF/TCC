package com.mareaalta.service;

import org.junit.Test;
import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingServiceTest {
    @Test
    public void testHighSeasonJuly() {
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 12, 15)));
    }

    @Test
    public void testHighSeasonJanuary() {
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 1, 1)));
    }

    @Test
    public void testHighSeasonFebruary() {
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testLowSeasonMarch() {
        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 3, 1)));
    }

    @Test
    public void testNightlyRateHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal highSeasonRate = PricingService.getNightlyRate(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), highSeasonRate);
    }

    @Test
    public void testNightlyRateLowSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal lowSeasonRate = PricingService.getNightlyRate(baseRate, LocalDate.of(2027, 3, 10));
        assertEquals(new BigDecimal("249.90"), lowSeasonRate);
    }

    @Test
    public void testCountNights() {
        assertEquals(2, PricingService.countNights(LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12)));
    }

    @Test
    public void testCalculateSubtotalHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = PricingService.calculateSubtotal(baseRate, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testCalculateDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = PricingService.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    public void testCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");
        BigDecimal total = PricingService.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("662.83"), total);
    }
}
