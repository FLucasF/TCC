package com.pousada.marealba.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class MoneyUtilTest {

    @Test
    public void testRoundHalfEven() {
        assertEquals(new BigDecimal("2.12"), MoneyUtil.round(2.125));
        assertEquals(new BigDecimal("2.14"), MoneyUtil.round(2.135));
        assertEquals(new BigDecimal("2.12"), MoneyUtil.round(2.115));
    }

    @Test
    public void testFormatBrazilian() {
        BigDecimal value = new BigDecimal("1234.56");
        String formatted = MoneyUtil.formatBrazilian(value);
        assertTrue(formatted.contains("1.234,56") || formatted.contains("1234,56"));
    }

    @Test
    public void testFormatCsv() {
        BigDecimal value = new BigDecimal("1234.56");
        assertEquals("1234.56", MoneyUtil.formatCsv(value));
    }
}
