package com.pousada.marealta;

import com.pousada.marealta.util.ReceiptFormatter;
import com.pousada.marealta.models.Receipt;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class IntegrationTest {

    @Test
    public void testReceiptFormatting() {
        Receipt receipt = new Receipt(
            7, "Carla Dias", "111.222.333-44", 201, "DELUXE",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2,
            new BigDecimal("779.80"), BigDecimal.ZERO, new BigDecimal("779.80")
        );

        String formatted = ReceiptFormatter.format(receipt);
        System.out.println(formatted);

        assertTrue(formatted.contains("Comprovante de reserva nº 7"));
        assertTrue(formatted.contains("Carla Dias"));
        assertTrue(formatted.contains("111.222.333-44"));
        assertTrue(formatted.contains("201"));
        assertTrue(formatted.contains("DELUXE"));
        assertTrue(formatted.contains("10/03/2027"));
        assertTrue(formatted.contains("12/03/2027"));
        assertTrue(formatted.contains("2 diárias"));
        assertTrue(formatted.contains("R$ 779.80"));
    }

    @Test
    public void testReceiptFormattingWithDiscount() {
        Receipt receipt = new Receipt(
            1, "Ana Silva", "123.456.789-00", 101, "STANDARD",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2,
            new BigDecimal("599.76"), new BigDecimal("89.96"), new BigDecimal("509.80")
        );

        String formatted = ReceiptFormatter.format(receipt);
        System.out.println(formatted);

        assertTrue(formatted.contains("Desconto: R$ 89.96"));
        assertTrue(formatted.contains("R$ 509.80"));
    }

    @Test
    public void testReceiptFormattingOneDayOnly() {
        Receipt receipt = new Receipt(
            1, "Ana Silva", "123.456.789-00", 301, "SUITE",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 1,
            new BigDecimal("579.90"), BigDecimal.ZERO, new BigDecimal("579.90")
        );

        String formatted = ReceiptFormatter.format(receipt);
        System.out.println(formatted);

        assertTrue(formatted.contains("1 diária"));
    }
}
