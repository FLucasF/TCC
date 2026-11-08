package com.pousadamarealta.util;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class MoneyUtilsTest {
    @Test
    public void testRoundingHalfEven() {
        assertEquals(new BigDecimal("10.50"), MoneyUtils.round(new BigDecimal("10.495")));
        assertEquals(new BigDecimal("10.50"), MoneyUtils.round(new BigDecimal("10.505")));
        assertEquals(new BigDecimal("10.52"), MoneyUtils.round(new BigDecimal("10.515")));
    }

    @Test
    public void testApplyHighSeasonRate() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal highSeasonRate = MoneyUtils.applyHighSeasonRate(baseRate);
        assertEquals(new BigDecimal("299.88"), highSeasonRate);
    }

    @Test
    public void testCalculateDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = MoneyUtils.calculateDiscount(subtotal, 15);
        assertEquals(new BigDecimal("89.96"), discount);
    }
}
