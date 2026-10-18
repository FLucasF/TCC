package com.loja.orderservice.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyUtilsTest {
    @Test
    void testRoundToCentsHalfEven() {
        assertEquals(new BigDecimal("3.00"), CurrencyUtils.roundToCents(new BigDecimal("2.995")));
        assertEquals(new BigDecimal("2.98"), CurrencyUtils.roundToCents(new BigDecimal("2.985")));
        assertEquals(new BigDecimal("2.50"), CurrencyUtils.roundToCents(new BigDecimal("2.505")));
    }

    @Test
    void testMultiply() {
        BigDecimal result = CurrencyUtils.multiply(new BigDecimal("79.90"), new BigDecimal("2"));
        assertEquals(new BigDecimal("159.80"), result);
    }

    @Test
    void testAdd() {
        BigDecimal result = CurrencyUtils.add(new BigDecimal("79.90"), new BigDecimal("20.00"));
        assertEquals(new BigDecimal("99.90"), result);
    }

    @Test
    void testDivide() {
        BigDecimal result = CurrencyUtils.divide(new BigDecimal("100.00"), new BigDecimal("3"));
        assertEquals(new BigDecimal("33.33"), result);
    }

    @Test
    void testSubtract() {
        BigDecimal result = CurrencyUtils.subtract(new BigDecimal("100.00"), new BigDecimal("30.00"));
        assertEquals(new BigDecimal("70.00"), result);
    }
}
