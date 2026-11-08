package com.pousada.util;

import org.junit.Test;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class PriceUtilTest {
    @Test
    public void testRounding() {
        BigDecimal value = new BigDecimal("249.895");
        BigDecimal rounded = PriceUtil.round(value);
        assertEquals(new BigDecimal("249.90"), rounded);
    }

    @Test
    public void testMultiply() {
        BigDecimal a = new BigDecimal("249.90");
        BigDecimal b = new BigDecimal("1.20");
        BigDecimal result = PriceUtil.multiply(a, b);
        assertEquals(new BigDecimal("299.88"), result);
    }

    @Test
    public void testHighSeasonMultiplier() {
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal highSeasonRate = PriceUtil.applyHighSeasonMultiplier(rate);
        assertEquals(new BigDecimal("299.88"), highSeasonRate);
    }

    @Test
    public void testApplyPercentage() {
        BigDecimal value = new BigDecimal("599.76");
        BigDecimal discount = PriceUtil.applyPercentage(value, new BigDecimal("0.15"));
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testSubtract() {
        BigDecimal a = new BigDecimal("599.76");
        BigDecimal b = new BigDecimal("89.96");
        BigDecimal result = PriceUtil.subtract(a, b);
        assertEquals(new BigDecimal("509.80"), result);
    }
}
