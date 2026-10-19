package com.pousadameraalta.util;

import org.junit.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class PriceCalculatorTest {

    @Test
    public void testLowSeasonPrice() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal result = PriceCalculator.calculateNightPrice(baseRate, date);
        assertEquals(new BigDecimal("249.90"), result);
    }

    @Test
    public void testHighSeasonPrice() {
        LocalDate date = LocalDate.of(2027, 7, 10);
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal result = PriceCalculator.calculateNightPrice(baseRate, date);
        assertEquals(new BigDecimal("299.88"), result);
    }

    @Test
    public void testHighSeasonDecemberToFebruary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 28).plusDays(1)));
    }

    @Test
    public void testHighSeasonJuly() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal nightPrice = new BigDecimal("299.88");
        BigDecimal result = PriceCalculator.calculateSubtotal(nightPrice, 2);
        assertEquals(new BigDecimal("599.76"), result);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal result = PriceCalculator.calculateDiscount(subtotal, true);
        assertEquals(new BigDecimal("89.96"), result);
    }

    @Test
    public void testStandardNoDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal result = PriceCalculator.calculateDiscount(subtotal, false);
        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal result = PriceCalculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), result);
    }

    @Test
    public void testIssCalculation() {
        BigDecimal total = new BigDecimal("779.80");
        BigDecimal result = PriceCalculator.calculateIss(total);
        assertEquals(new BigDecimal("38.99"), result);
    }

    @Test
    public void testNightsCalculation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        int nights = PriceCalculator.calculateNights(checkIn, checkOut);
        assertEquals(2, nights);
    }

    @Test
    public void testRoundingToEvenCents() {
        assertEquals(new BigDecimal("10.02"), PriceCalculator.roundToEvenCents(new BigDecimal("10.015")));
        assertEquals(new BigDecimal("10.02"), PriceCalculator.roundToEvenCents(new BigDecimal("10.025")));
        assertEquals(new BigDecimal("10.04"), PriceCalculator.roundToEvenCents(new BigDecimal("10.035")));
    }
}
