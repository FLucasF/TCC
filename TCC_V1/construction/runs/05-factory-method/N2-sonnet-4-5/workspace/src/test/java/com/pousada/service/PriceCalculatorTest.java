package com.pousada.service;

import com.pousada.domain.RatePlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PriceCalculatorTest {

    @Test
    void testHighSeasonJuly() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testHighSeasonDecemberFrom15() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));
    }

    @Test
    void testHighSeasonJanuaryFebruary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void testLowSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 1)));
    }

    @Test
    void testNightPriceHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);
        BigDecimal expected = new BigDecimal("299.88");
        assertEquals(expected, PriceCalculator.calculateNightPrice(baseRate, highSeasonDate));
    }

    @Test
    void testNightPriceLowSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate lowSeasonDate = LocalDate.of(2027, 3, 10);
        assertEquals(baseRate, PriceCalculator.calculateNightPrice(baseRate, lowSeasonDate));
    }

    @Test
    void testSubtotalTwoNightsHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal expected = new BigDecimal("599.76");
        assertEquals(expected, PriceCalculator.calculateSubtotal(baseRate, checkIn, checkOut));
    }

    @Test
    void testDiscountNonRefundable() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal expected = new BigDecimal("116.97");
        assertEquals(expected, PriceCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE));
    }

    @Test
    void testDiscountStandard() {
        BigDecimal subtotal = new BigDecimal("779.80");
        assertEquals(BigDecimal.ZERO, PriceCalculator.calculateDiscount(subtotal, RatePlan.STANDARD));
    }

    @Test
    void testTotal() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");
        BigDecimal expected = new BigDecimal("662.83");
        assertEquals(expected, PriceCalculator.calculateTotal(subtotal, discount));
    }
}
