package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PriceCalculatorTest {

    @Test
    void testHighSeasonInDecember() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    void testHighSeasonInJanuaryAndFebruary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 31)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void testHighSeasonInJuly() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testLowSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void testNightPriceCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");

        BigDecimal lowSeasonPrice = PriceCalculator.calculateNightPrice(baseRate, LocalDate.of(2027, 3, 10));
        assertEquals(new BigDecimal("249.90"), lowSeasonPrice);

        BigDecimal highSeasonPrice = PriceCalculator.calculateNightPrice(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), highSeasonPrice);
    }

    @Test
    void testSubtotalCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = PriceCalculator.calculateSubtotal(baseRate, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testDiscountCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal noDiscount = PriceCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO.setScale(2), noDiscount);

        BigDecimal withDiscount = PriceCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), withDiscount);
    }
}
