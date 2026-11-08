package com.pousadamarealta;

import com.pousadamarealta.utils.FormatterUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FormatterUtilsTest {

    @Test
    public void testFormatCpf() {
        String cpf = "12345678901";
        String formatted = FormatterUtils.formatCpf(cpf);
        assertEquals("123.456.789-01", formatted);
    }

    @Test
    public void testFormatDate() {
        var date = java.time.LocalDate.of(2027, 3, 10);
        String formatted = FormatterUtils.formatDate(date);
        assertEquals("10/03/2027", formatted);
    }

    @Test
    public void testFormatCsvDate() {
        var date = java.time.LocalDate.of(2027, 7, 10);
        String formatted = FormatterUtils.formatCsvDate(date);
        assertEquals("2027-07-10", formatted);
    }
}
