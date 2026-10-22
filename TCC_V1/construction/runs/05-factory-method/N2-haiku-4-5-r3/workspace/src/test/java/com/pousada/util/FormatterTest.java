package com.pousada.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FormatterTest {

    @Test
    void testBrDate() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        assertEquals("10/03/2027", Formatter.formatBrDate(date));
    }

    @Test
    void testEnDate() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        assertEquals("2027-03-10", Formatter.formatEnDate(date));
    }

    @Test
    void testBrCurrency() {
        BigDecimal value = new BigDecimal("1234.56");
        assertEquals("R$ 1.234,56", Formatter.formatBrCurrency(value));
    }

    @Test
    void testEnCurrency() {
        BigDecimal value = new BigDecimal("1234.56");
        assertEquals("BRL 1234.56", Formatter.formatEnCurrency(value));
    }

    @Test
    void testFormatCpf() {
        assertEquals("111.222.333-44", Formatter.formatCpf("11122233344"));
    }

    @Test
    void testFormatCnpj() {
        assertEquals("12.345.678/0001-90", Formatter.formatCnpj("12345678000190"));
    }
}
