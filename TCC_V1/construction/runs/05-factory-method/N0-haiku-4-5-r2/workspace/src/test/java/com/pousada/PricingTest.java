package com.pousada;

import com.pousada.util.Pricing;
import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingTest {
    @Test
    public void testPeakSeasonJuly() {
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    public void testPeakSeasonDecember() {
        assertFalse(Pricing.isPeakSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2027, 12, 31)));
    }

    @Test
    public void testPeakSeasonJanuary() {
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2027, 1, 31)));
    }

    @Test
    public void testPeakSeasonFebruary() {
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2027, 2, 1)));
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testNightlyRateInPeakSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate peakDate = LocalDate.of(2027, 7, 10);
        BigDecimal rate = Pricing.calculateNightlyRate(baseRate, peakDate);
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    public void testNightlyRateOutOfPeakSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate lowDate = LocalDate.of(2027, 5, 10);
        BigDecimal rate = Pricing.calculateNightlyRate(baseRate, lowDate);
        assertEquals(new BigDecimal("249.90"), rate);
    }

    @Test
    public void testDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = Pricing.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testNoDiscountForStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = Pricing.calculateDiscount(subtotal, "STANDARD");
        assertEquals(0, discount.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = Pricing.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }

    @Test
    public void testISS() {
        BigDecimal total = new BigDecimal("779.80");
        BigDecimal iss = Pricing.calculateIss(total);
        assertEquals(new BigDecimal("38.99"), iss);
    }
}
