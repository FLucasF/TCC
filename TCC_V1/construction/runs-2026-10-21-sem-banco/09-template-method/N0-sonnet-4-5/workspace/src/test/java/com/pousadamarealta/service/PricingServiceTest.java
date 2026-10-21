package com.pousadamarealta.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void testHighSeasonDecember() {
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    void testHighSeasonJanuaryFebruary() {
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void testHighSeasonJuly() {
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 6, 30)));
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void testCalculateNightPriceRegularSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 3, 10);
        BigDecimal price = pricingService.calculateNightPrice(baseRate, date);
        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    void testCalculateNightPriceHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 7, 10);
        BigDecimal price = pricingService.calculateNightPrice(baseRate, date);
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void testRoundingHalfEven() {
        assertEquals(new BigDecimal("1.22"), pricingService.round(new BigDecimal("1.225")));
        assertEquals(new BigDecimal("1.24"), pricingService.round(new BigDecimal("1.235")));
        assertEquals(new BigDecimal("1.23"), pricingService.round(new BigDecimal("1.2250001")));
    }

    @Test
    void testCalculateTourismTax() {
        BigDecimal tax = pricingService.calculateTourismTax(2, 3);
        assertEquals(new BigDecimal("27.00"), tax);
    }
}
