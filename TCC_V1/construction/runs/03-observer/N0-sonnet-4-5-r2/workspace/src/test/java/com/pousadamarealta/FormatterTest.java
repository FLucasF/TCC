package com.pousadamarealta;

import com.pousadamarealta.util.Formatter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class FormatterTest {

    @Test
    public void testFormatMoney() {
        assertEquals("R$ 1.234,56", Formatter.formatMoney(new BigDecimal("1234.56")));
        assertEquals("R$ 599,76", Formatter.formatMoney(new BigDecimal("599.76")));
        assertEquals("R$ 0,00", Formatter.formatMoney(BigDecimal.ZERO));
    }

    @Test
    public void testFormatDate() {
        assertEquals("10/03/2027", Formatter.formatDate(LocalDate.of(2027, 3, 10)));
        assertEquals("15/12/2026", Formatter.formatDate(LocalDate.of(2026, 12, 15)));
    }

    @Test
    public void testFormatCpf() {
        assertEquals("111.222.333-44", Formatter.formatCpf("11122233344"));
        assertEquals("123.456.789-01", Formatter.formatCpf("12345678901"));
    }
}
