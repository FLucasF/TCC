package com.pousada.marealta.service;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceCalculatorTest {
    @Test
    public void testNightlyPriceHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate dateJuly = LocalDate.of(2027, 7, 10);

        BigDecimal result = PriceCalculator.calculateNightlyPrice(baseRate, dateJuly);

        BigDecimal expected = new BigDecimal("299.88");
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testNightlyPriceLowSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate dateApril = LocalDate.of(2027, 4, 10);

        BigDecimal result = PriceCalculator.calculateNightlyPrice(baseRate, dateApril);

        BigDecimal expected = new BigDecimal("249.90");
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testSubtotalTwoNights() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal result = PriceCalculator.calculateSubtotal(baseRate, checkIn, checkOut);

        BigDecimal expected = new BigDecimal("599.76");
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testDiscountNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal result = PriceCalculator.calculateDiscount(subtotal, true);

        BigDecimal expected = new BigDecimal("89.96");
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testDiscountStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal result = PriceCalculator.calculateDiscount(subtotal, false);

        BigDecimal expected = BigDecimal.ZERO.setScale(2);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal result = PriceCalculator.calculateTotal(subtotal, discount);

        BigDecimal expected = new BigDecimal("509.80");
        Assert.assertEquals(expected, result);
    }
}
