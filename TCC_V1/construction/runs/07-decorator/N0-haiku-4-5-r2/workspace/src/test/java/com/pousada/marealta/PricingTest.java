package com.pousada.marealta;

import com.pousada.marealta.util.PricingUtil;
import org.junit.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class PricingTest {

    @Test
    public void testHighSeasonJuly() {
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertFalse(PricingUtil.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2027, 12, 31)));
    }

    @Test
    public void testHighSeasonJanFeb() {
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(PricingUtil.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testLowSeason() {
        assertFalse(PricingUtil.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertFalse(PricingUtil.isHighSeason(LocalDate.of(2027, 6, 30)));
    }

    @Test
    public void testNightlyPriceHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal result = PricingUtil.calculateNightlyPrice(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), result);
    }

    @Test
    public void testNightlyPriceLowSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal result = PricingUtil.calculateNightlyPrice(baseRate, LocalDate.of(2027, 3, 10));
        assertEquals(new BigDecimal("249.90"), result);
    }

    @Test
    public void testCountNights() {
        int nights = PricingUtil.countNights(LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(2, nights);
    }

    @Test
    public void testSubtotalHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal result = PricingUtil.calculateSubtotal(baseRate, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(new BigDecimal("599.76"), result);
    }

    @Test
    public void testDiscountNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal result = PricingUtil.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(new BigDecimal("89.96"), result);
    }

    @Test
    public void testDiscountStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal result = PricingUtil.calculateDiscount(subtotal, "STANDARD");
        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal result = PricingUtil.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), result);
    }
}
