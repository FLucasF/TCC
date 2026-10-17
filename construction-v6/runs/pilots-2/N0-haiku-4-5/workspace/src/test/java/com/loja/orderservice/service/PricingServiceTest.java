package com.loja.orderservice.service;

import com.loja.orderservice.util.PricingUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    @Test
    void testRoundingHalfEven() {
        BigDecimal value = new BigDecimal("2.995");
        BigDecimal rounded = PricingUtil.round(value);
        assertEquals(new BigDecimal("3.00"), rounded);

        BigDecimal value2 = new BigDecimal("2.985");
        BigDecimal rounded2 = PricingUtil.round(value2);
        assertEquals(new BigDecimal("2.98"), rounded2);
    }

    @Test
    void testPercentage() {
        BigDecimal base = new BigDecimal("100.00");
        BigDecimal percent = new BigDecimal("10");
        BigDecimal result = PricingUtil.percentage(base, percent);
        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void testCeilDays() {
        int days = PricingUtil.ceilDays(41);
        assertEquals(2, days);

        int days2 = PricingUtil.ceilDays(24);
        assertEquals(1, days2);

        int days3 = PricingUtil.ceilDays(48);
        assertEquals(2, days3);
    }

    @Test
    void testAdd() {
        BigDecimal a = new BigDecimal("10.00");
        BigDecimal b = new BigDecimal("20.00");
        BigDecimal result = PricingUtil.add(a, b);
        assertEquals(new BigDecimal("30.00"), result);
    }

    @Test
    void testSubtract() {
        BigDecimal a = new BigDecimal("30.00");
        BigDecimal b = new BigDecimal("10.00");
        BigDecimal result = PricingUtil.subtract(a, b);
        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    void testMultiply() {
        BigDecimal a = new BigDecimal("10.00");
        BigDecimal b = new BigDecimal("2.00");
        BigDecimal result = PricingUtil.multiply(a, b);
        assertEquals(new BigDecimal("20.00"), result);
    }
}
