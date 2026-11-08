package com.marealata.util;

import com.marealata.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentFormatterTest {
    @Test
    void testFormatCurrency() {
        assertEquals("R$ 249,90", DocumentFormatter.formatCurrency(new BigDecimal("249.90")));
        assertEquals("R$ 599,76", DocumentFormatter.formatCurrency(new BigDecimal("599.76")));
    }

    @Test
    void testFormatDate() {
        assertEquals("10/07/2027", DocumentFormatter.formatDate(LocalDate.of(2027, 7, 10)));
        assertEquals("01/03/2027", DocumentFormatter.formatDate(LocalDate.of(2027, 3, 1)));
    }

    @Test
    void testFormatCpf() {
        assertEquals("111.222.333-44", DocumentFormatter.formatCpf("11122233344"));
    }

    @Test
    void testReservationDocument() {
        Reservation res = new Reservation();
        res.id = 7L;
        res.guestName = "Carla Dias";
        res.guestCpf = "11122233344";
        res.roomNumber = 201;
        res.checkIn = LocalDate.of(2027, 3, 10);
        res.checkOut = LocalDate.of(2027, 3, 12);
        res.nights = 2;
        res.subtotal = new BigDecimal("779.80");
        res.discount = BigDecimal.ZERO;
        res.total = new BigDecimal("779.80");

        String doc = DocumentFormatter.formatReservationDocument(res, "DELUXE");
        assertTrue(doc.contains("Comprovante de reserva nº 7"));
        assertTrue(doc.contains("Carla Dias"));
        assertTrue(doc.contains("111.222.333-44"));
        assertTrue(doc.contains("201"));
        assertTrue(doc.contains("DELUXE"));
        assertTrue(doc.contains("2 diárias"));
    }
}
