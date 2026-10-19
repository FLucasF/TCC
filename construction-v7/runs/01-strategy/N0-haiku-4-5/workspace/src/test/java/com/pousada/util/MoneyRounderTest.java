package com.pousada.util;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class MoneyRounderTest {

    @Test
    public void testRoundHalfEven() {
        assertEquals(new BigDecimal("1.20"), MoneyRounder.round(new BigDecimal("1.195")));
        assertEquals(new BigDecimal("1.22"), MoneyRounder.round(new BigDecimal("1.215")));
    }

    @Test
    public void testPercentage() {
        assertEquals(new BigDecimal("93.58"), MoneyRounder.percentage(new BigDecimal("779.80"), 12));
        assertEquals(new BigDecimal("174.93"), MoneyRounder.percentage(new BigDecimal("1749.30"), 10));
    }

    @Test
    public void testAdd() {
        assertEquals(new BigDecimal("249.90"), MoneyRounder.add(new BigDecimal("249.90"), BigDecimal.ZERO));
    }

    @Test
    public void testSubtract() {
        assertEquals(new BigDecimal("686.22"), MoneyRounder.subtract(new BigDecimal("779.80"), new BigDecimal("93.58")));
    }

    @Test
    public void testHighSeasonMultiply() {
        assertEquals(new BigDecimal("299.88"), MoneyRounder.multiply(new BigDecimal("249.90"), new BigDecimal("1.2")));
    }
}
