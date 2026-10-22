package com.pousadamarealta.util;

import org.junit.Test;
import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingUtilTest {
    @Test
    public void testHighSeasonJuly() {
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2027, 7, 15)));
    }

    @Test
    public void testHighSeasonDecToFeb() {
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2026, 12, 15)));
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2026, 12, 25)));
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2027, 2, 28)));
        assertFalse(PricingUtil.isHighSeason(LocalDate.of(2026, 12, 14)));
    }

    @Test
    public void testLowSeason() {
        assertFalse(PricingUtil.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(PricingUtil.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertFalse(PricingUtil.isHighSeason(LocalDate.of(2027, 11, 15)));
    }

    @Test
    public void testCalculateNightPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");

        LocalDate lowSeason = LocalDate.of(2027, 6, 10);
        BigDecimal lowSeasonPrice = PricingUtil.calculateNightPrice(baseRate, lowSeason);
        assertEquals(new BigDecimal("249.90"), lowSeasonPrice);

        LocalDate highSeason = LocalDate.of(2027, 7, 10);
        BigDecimal highSeasonPrice = PricingUtil.calculateNightPrice(baseRate, highSeason);
        assertEquals(new BigDecimal("299.88"), highSeasonPrice);
    }

    @Test
    public void testCalculateSubtotal() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = PricingUtil.calculateSubtotal(baseRate, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testCalculateDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal standardDiscount = PricingUtil.calculateDiscount(subtotal, "STANDARD");
        assertEquals(BigDecimal.ZERO, standardDiscount);

        BigDecimal nonRefundableDiscount = PricingUtil.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(new BigDecimal("89.96"), nonRefundableDiscount);
    }

    @Test
    public void testRounding() {
        BigDecimal value = new BigDecimal("249.895");
        BigDecimal rounded = PricingUtil.round(value);
        assertEquals(new BigDecimal("249.90"), rounded);

        BigDecimal value2 = new BigDecimal("249.894");
        BigDecimal rounded2 = PricingUtil.round(value2);
        assertEquals(new BigDecimal("249.89"), rounded2);
    }

    @Test
    public void testCalculateNights() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        assertEquals(2, PricingUtil.calculateNights(checkIn, checkOut));

        LocalDate checkOut2 = LocalDate.of(2027, 7, 11);
        assertEquals(1, PricingUtil.calculateNights(checkIn, checkOut2));
    }
}
