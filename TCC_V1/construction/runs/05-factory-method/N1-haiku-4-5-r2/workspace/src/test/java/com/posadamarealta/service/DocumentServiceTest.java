package com.posadamarealta.service;

import com.posadamarealta.model.Document;
import com.posadamarealta.model.Reservation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentServiceTest {

    @Test
    public void testGenerateReceiptWithCPF() {
        Reservation res = new Reservation(
            7, 201, "Carla Dias", "carla@email.com",
            "11122233344", null, null, null, null,
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, "STANDARD",
            2, new BigDecimal("779.80"), BigDecimal.ZERO, new BigDecimal("779.80"),
            "CONFIRMED", new BigDecimal("779.80"), null
        );

        Document doc = DocumentService.generate(res, "DELUXE");

        assertEquals("Comprovante de reserva", doc.title);
        assertNotNull(doc.text);
        assertTrue(doc.text.contains("Comprovante de reserva nº 7"));
        assertTrue(doc.text.contains("Carla Dias"));
        assertTrue(doc.text.contains("CPF 111.222.333-44"));
        assertTrue(doc.text.contains("201 (DELUXE)"));
        assertTrue(doc.text.contains("R$ 779,80"));
    }

    @Test
    public void testGenerateInvoiceWithCNPJ() {
        Reservation res = new Reservation(
            3, 201, "Ana Souza", "ana@email.com",
            null, "12345678000190", "Tecnosul Ltda", null, null,
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, "STANDARD",
            2, new BigDecimal("779.80"), BigDecimal.ZERO, new BigDecimal("779.80"),
            "CONFIRMED", new BigDecimal("779.80"), null
        );

        Document doc = DocumentService.generate(res, "DELUXE");

        assertEquals("Fatura de hospedagem", doc.title);
        assertNotNull(doc.text);
        assertTrue(doc.text.contains("Fatura de hospedagem nº 3"));
        assertTrue(doc.text.contains("Tecnosul Ltda"));
        assertTrue(doc.text.contains("CNPJ 12.345.678/0001-90"));
        assertTrue(doc.text.contains("ISS retido (5%)"));
    }

    @Test
    public void testGenerateBookingReceiptWithPassport() {
        Reservation res = new Reservation(
            4, 201, "John Smith", "john@email.com",
            null, null, null, "X1234567", "United Kingdom",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, "STANDARD",
            2, new BigDecimal("779.80"), BigDecimal.ZERO, new BigDecimal("779.80"),
            "CONFIRMED", new BigDecimal("779.80"), null
        );

        Document doc = DocumentService.generate(res, "DELUXE");

        assertEquals("Booking receipt", doc.title);
        assertNotNull(doc.text);
        assertTrue(doc.text.contains("Booking receipt no. 4"));
        assertTrue(doc.text.contains("John Smith"));
        assertTrue(doc.text.contains("passport X1234567"));
        assertTrue(doc.text.contains("United Kingdom"));
        assertTrue(doc.text.contains("BRL 779.80"));
    }

    @Test
    public void testGenerateWithDiscount() {
        Reservation res = new Reservation(
            1, 101, "Test Guest", "test@email.com",
            "11122233344", null, null, null, null,
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 1, "NON_REFUNDABLE",
            2, new BigDecimal("599.76"), new BigDecimal("89.96"), new BigDecimal("509.80"),
            "CONFIRMED", new BigDecimal("509.80"), null
        );

        Document doc = DocumentService.generate(res, "STANDARD");

        assertTrue(doc.text.contains("Desconto:"));
    }
}
