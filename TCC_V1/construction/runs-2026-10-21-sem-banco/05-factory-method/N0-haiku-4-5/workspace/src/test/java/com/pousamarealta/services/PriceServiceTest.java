package com.pousamarealta.services;

import org.junit.Test;
import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceServiceTest {

    @Test
    public void testHighSeasonJuly() {
        assertTrue(PriceService.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertTrue(PriceService.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertFalse(PriceService.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    public void testHighSeasonJanFeb() {
        assertTrue(PriceService.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(PriceService.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testNightlyRateHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal highSeasonRate = PriceService.calculateNightlyRate(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), highSeasonRate);
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = PriceService.calculateSubtotal(baseRate,
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testDiscountCalculation() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = PriceService.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    public void testNightsCalculation() {
        int nights = PriceService.calculateNights(
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(2, nights);
    }
}
