package com.pousada.mare_alta.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class NumberFormatterTest {
    @Test
    public void testRoundHalfEven() {
        // Should round up
        BigDecimal value = new BigDecimal("100.125");
        BigDecimal rounded = NumberFormatter.round(value);
        assertEquals(new BigDecimal("100.12"), rounded);

        // Should round down
        value = new BigDecimal("100.124");
        rounded = NumberFormatter.round(value);
        assertEquals(new BigDecimal("100.12"), rounded);
    }

    @Test
    public void testFormatCurrency() {
        BigDecimal value = new BigDecimal("1234.56");
        String formatted = NumberFormatter.formatCurrency(value);
        assertTrue(formatted.contains("1.234,56"));
    }

    @Test
    public void testFormatCurrencyForJson() {
        BigDecimal value = new BigDecimal("1234.56");
        String formatted = NumberFormatter.formatCurrencyForJson(value);
        assertEquals("1234.56", formatted);
    }
}
