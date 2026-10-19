package com.pousadamarealta.service;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class PriceCalculatorTest {

    @Test
    public void testNormalSeasonPrice() {
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal price = PriceCalculator.calculateNightlyPrice(rate, LocalDate.of(2027, 5, 15));
        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    public void testHighSeasonJuly() {
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal price = PriceCalculator.calculateNightlyPrice(rate, LocalDate.of(2027, 7, 10));
        // 249.90 * 1.20 = 299.88
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    public void testHighSeasonDecember() {
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal price = PriceCalculator.calculateNightlyPrice(rate, LocalDate.of(2027, 12, 25));
        // 249.90 * 1.20 = 299.88
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    public void testLowSeasonDecember() {
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal price = PriceCalculator.calculateNightlyPrice(rate, LocalDate.of(2027, 12, 10));
        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    public void testHighSeasonJanuary() {
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal price = PriceCalculator.calculateNightlyPrice(rate, LocalDate.of(2027, 1, 15));
        // 249.90 * 1.20 = 299.88
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    public void testHighSeasonFebruary() {
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal price = PriceCalculator.calculateNightlyPrice(rate, LocalDate.of(2027, 2, 28));
        // 249.90 * 1.20 = 299.88
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    public void testLowSeasonMarch() {
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal price = PriceCalculator.calculateNightlyPrice(rate, LocalDate.of(2027, 3, 1));
        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    public void testStandardRatePlan() {
        BigDecimal rate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PriceCalculator.PriceResult result = PriceCalculator.calculate(rate, checkIn, checkOut, "STANDARD");

        // 2 nights * 299.88 = 599.76
        assertEquals(new BigDecimal("599.76"), result.subtotal);
        assertEquals(BigDecimal.ZERO, result.discount);
        assertEquals(new BigDecimal("599.76"), result.total);
    }

    @Test
    public void testNonRefundableRatePlan() {
        BigDecimal rate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PriceCalculator.PriceResult result = PriceCalculator.calculate(rate, checkIn, checkOut, "NON_REFUNDABLE");

        // 2 nights * 299.88 = 599.76, discount = 599.76 * 0.15 = 89.964 -> 89.96
        assertEquals(new BigDecimal("599.76"), result.subtotal);
        assertEquals(new BigDecimal("89.96"), result.discount);
        assertEquals(new BigDecimal("509.80"), result.total);
    }

    @Test
    public void testRoundingHalfEven() {
        BigDecimal rate = new BigDecimal("389.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PriceCalculator.PriceResult result = PriceCalculator.calculate(rate, checkIn, checkOut, "STANDARD");

        // 2 nights * 467.88 = 935.76
        assertEquals(new BigDecimal("935.76"), result.subtotal);
        assertEquals(BigDecimal.ZERO, result.discount);
        assertEquals(new BigDecimal("935.76"), result.total);
    }
}
