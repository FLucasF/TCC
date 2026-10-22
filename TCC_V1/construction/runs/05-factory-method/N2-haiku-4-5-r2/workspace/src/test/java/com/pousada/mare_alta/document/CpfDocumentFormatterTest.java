package com.pousada.mare_alta.document;

import com.pousada.mare_alta.model.Document;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class CpfDocumentFormatterTest {
    @Test
    public void testFormatDocument() {
        DocumentData data = new DocumentData(
                7, "Carla Dias", "11122233344", null, null, null, null,
                201, "DELUXE", LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2,
                new BigDecimal("779.80"), BigDecimal.ZERO, new BigDecimal("779.80")
        );

        DocumentFormatter formatter = new CpfDocumentFormatter();
        Document doc = formatter.format(data);

        assertEquals("Comprovante de reserva", doc.getTitle());
        assertEquals(7, doc.getReservationId());
        assertEquals("Carla Dias", doc.getGuestName());
        assertTrue(doc.getText().contains("Comprovante de reserva nº 7"));
        assertTrue(doc.getText().contains("CPF 111.222.333-44"));
        assertTrue(doc.getText().contains("Quarto: 201 (DELUXE)"));
        assertTrue(doc.getText().contains("2 diárias"));
        assertTrue(doc.getText().contains("R$ 779,80"));
    }

    @Test
    public void testFormatDocumentWithDiscount() {
        DocumentData data = new DocumentData(
                1, "John Smith", "12345678901", null, null, null, null,
                101, "STANDARD", LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2,
                new BigDecimal("500.00"), new BigDecimal("75.00"), new BigDecimal("425.00")
        );

        DocumentFormatter formatter = new CpfDocumentFormatter();
        Document doc = formatter.format(data);

        assertTrue(doc.getText().contains("Desconto:"));
        assertTrue(doc.getText().contains("R$ 75,00"));
    }
}
