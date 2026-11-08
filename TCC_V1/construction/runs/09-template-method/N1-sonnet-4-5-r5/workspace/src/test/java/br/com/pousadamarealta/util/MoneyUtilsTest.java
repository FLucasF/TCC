package br.com.pousadamarealta.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoneyUtilsTest {

    @Test
    public void testRoundHalfEven() {
        // Banker's rounding: 0.5 arredonda para o número par mais próximo
        assertEquals(new BigDecimal("2.00"), MoneyUtils.round(new BigDecimal("2.005")));
        assertEquals(new BigDecimal("3.00"), MoneyUtils.round(new BigDecimal("3.005")));
        assertEquals(new BigDecimal("2.02"), MoneyUtils.round(new BigDecimal("2.015")));
        assertEquals(new BigDecimal("2.02"), MoneyUtils.round(new BigDecimal("2.025")));
    }

    @Test
    public void testFormatCurrency() {
        assertEquals("R$ 249,90", MoneyUtils.formatCurrency(new BigDecimal("249.90")));
        assertEquals("R$ 1.234,56", MoneyUtils.formatCurrency(new BigDecimal("1234.56")));
    }

    @Test
    public void testFormatCsv() {
        assertEquals("249.90", MoneyUtils.formatCsv(new BigDecimal("249.90")));
        assertEquals("1234.56", MoneyUtils.formatCsv(new BigDecimal("1234.56")));
    }
}
