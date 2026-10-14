package com.loja.pedidos.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyUtilTest {
    @Test
    void testRoundingHalfEven() {
        // 2.995 should round to 3.00 (even)
        BigDecimal value1 = new BigDecimal("2.995");
        assertEquals(new BigDecimal("3.00"), MoneyUtil.roundToNearestEven(value1));

        // 2.985 should round to 2.98 (even)
        BigDecimal value2 = new BigDecimal("2.985");
        assertEquals(new BigDecimal("2.98"), MoneyUtil.roundToNearestEven(value2));

        // 2.975 should round to 2.98 (even)
        BigDecimal value3 = new BigDecimal("2.975");
        assertEquals(new BigDecimal("2.98"), MoneyUtil.roundToNearestEven(value3));
    }

    @Test
    void testPercentageCalculation() {
        BigDecimal base = new BigDecimal("100.00");
        BigDecimal result = MoneyUtil.percentageOf(base, new BigDecimal("10"));
        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void testAdd() {
        BigDecimal result = MoneyUtil.add(
            new BigDecimal("10.00"),
            new BigDecimal("20.00"),
            new BigDecimal("30.00")
        );
        assertEquals(new BigDecimal("60.00"), result);
    }
}
