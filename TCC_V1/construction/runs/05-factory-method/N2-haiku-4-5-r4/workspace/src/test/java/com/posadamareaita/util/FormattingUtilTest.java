package com.posadamareaita.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FormattingUtilTest {
    @Test
    public void testFormatMoneyPT() {
        assertEquals("R$ 249,90", FormattingUtil.formatMoneyPT(new BigDecimal("249.90")));
        assertEquals("R$ 1.234,56", FormattingUtil.formatMoneyPT(new BigDecimal("1234.56")));
    }

    @Test
    public void testFormatMoneyEN() {
        assertEquals("BRL 249.90", FormattingUtil.formatMoneyEN(new BigDecimal("249.90")));
        assertEquals("BRL 1,234.56", FormattingUtil.formatMoneyEN(new BigDecimal("1234.56")));
    }

    @Test
    public void testFormatDatePT() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        assertEquals("10/03/2027", FormattingUtil.formatDatePT(date));
    }

    @Test
    public void testFormatDateISO() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        assertEquals("2027-03-10", FormattingUtil.formatDateISO(date));
    }

    @Test
    public void testFormatDateEN() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        assertEquals("2027-03-10", FormattingUtil.formatDateEN(date));
    }

    @Test
    public void testFormatNightsPT() {
        assertEquals("1 diária", FormattingUtil.formatNightsPT(1));
        assertEquals("2 diárias", FormattingUtil.formatNightsPT(2));
    }

    @Test
    public void testFormatNightsEN() {
        assertEquals("1 night", FormattingUtil.formatNightsEN(1));
        assertEquals("2 nights", FormattingUtil.formatNightsEN(2));
    }
}
