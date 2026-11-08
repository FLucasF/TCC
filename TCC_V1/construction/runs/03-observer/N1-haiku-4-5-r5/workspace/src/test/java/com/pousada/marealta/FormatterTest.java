package com.pousada.marealta;

import com.pousada.marealta.util.Formatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class FormatterTest {
    @Test
    public void testFormatDate() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        assertEquals("10/03/2027", Formatter.formatDate(date));
    }

    @Test
    public void testFormatMoney() {
        BigDecimal amount = new BigDecimal("1234.56");
        assertEquals("R$ 1.234,56", Formatter.formatMoney(amount));
    }

    @Test
    public void testFormatMoneyCSV() {
        BigDecimal amount = new BigDecimal("1234.56");
        assertEquals("1234.56", Formatter.formatMoneyCSV(amount));
    }

    @Test
    public void testFormatDateCSV() {
        LocalDate date = LocalDate.of(2027, 7, 10);
        assertEquals("2027-07-10", Formatter.formatDateCSV(date));
    }

    @Test
    public void testFormatCPF() {
        String cpf = "11122233344";
        assertEquals("111.222.333-44", Formatter.formatCPF(cpf));
    }

    @Test
    public void testUnformatCPF() {
        String cpf = "111.222.333-44";
        assertEquals("11122233344", Formatter.unformatCPF(cpf));
    }
}
