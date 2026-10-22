package com.pousadamarealta.util;

import org.junit.Test;
import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingTest {
    @Test
    public void testHighSeasonJuly() {
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    public void testHighSeasonJanuary() {
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 1, 15)));
    }

    @Test
    public void testHighSeasonFebruary() {
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 2, 15)));
    }

    @Test
    public void testLowSeason() {
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 3, 15)));
    }

    @Test
    public void testNightlyRateHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal rate = Pricing.calculateNightlyRate(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    public void testNightlyRateLowSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal rate = Pricing.calculateNightlyRate(baseRate, LocalDate.of(2027, 3, 10));
        assertEquals(new BigDecimal("249.90"), rate);
    }

    @Test
    public void testGetNights() {
        int nights = Pricing.getNights(LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(2, nights);
    }

    @Test
    public void testCalculateSubtotal() {
        BigDecimal subtotal = Pricing.calculateSubtotal(
                new BigDecimal("249.90"),
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12)
        );
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testCalculateDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = Pricing.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testNoDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = Pricing.calculateDiscount(subtotal, "STANDARD");
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    public void testCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = Pricing.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }
}
