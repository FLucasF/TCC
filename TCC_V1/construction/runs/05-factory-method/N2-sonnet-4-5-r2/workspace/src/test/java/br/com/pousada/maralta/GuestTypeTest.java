package br.com.pousada.maralta;

import br.com.pousada.maralta.domain.Document;
import br.com.pousada.maralta.guesttype.CorporateGuest;
import br.com.pousada.maralta.guesttype.ForeignGuest;
import br.com.pousada.maralta.guesttype.IndividualGuest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class GuestTypeTest {

    @Test
    void testIndividualGuestDocument() {
        IndividualGuest guest = new IndividualGuest("111.222.333-44");

        Document doc = guest.generateDocument(
            7L,
            "Carla Dias",
            "201",
            "DELUXE",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        assertTrue(doc.getText().contains("Comprovante de reserva nº 7"));
        assertTrue(doc.getText().contains("Carla Dias (CPF 111.222.333-44)"));
        assertTrue(doc.getText().contains("201 (DELUXE)"));
        assertTrue(doc.getText().contains("2 diárias"));
        assertTrue(doc.getText().contains("R$ 779,80"));
    }

    @Test
    void testCorporateGuestDocument() {
        CorporateGuest guest = new CorporateGuest("12.345.678/0001-90", "Tecnosul Ltda");

        Document doc = guest.generateDocument(
            3L,
            "Ana Souza",
            "201",
            "DELUXE",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        assertTrue(doc.getText().contains("Fatura de hospedagem nº 3"));
        assertTrue(doc.getText().contains("Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(doc.getText().contains("Hóspede: Ana Souza"));
        assertTrue(doc.getText().contains("ISS retido (5%): R$ 38,99"));
    }

    @Test
    void testForeignGuestDocument() {
        ForeignGuest guest = new ForeignGuest("X1234567", "United Kingdom");

        Document doc = guest.generateDocument(
            4L,
            "John Smith",
            "201",
            "DELUXE",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        assertTrue(doc.getText().contains("Booking receipt no. 4"));
        assertTrue(doc.getText().contains("John Smith (passport X1234567, United Kingdom)"));
        assertTrue(doc.getText().contains("2027-03-10"));
        assertTrue(doc.getText().contains("2 nights"));
        assertTrue(doc.getText().contains("BRL 779.80"));
    }

    @Test
    void testIndividualGuestWithDiscount() {
        IndividualGuest guest = new IndividualGuest("111.222.333-44");

        Document doc = guest.generateDocument(
            1L,
            "Test User",
            "101",
            "STANDARD",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 11),
            1,
            new BigDecimal("249.90"),
            new BigDecimal("37.49"),
            new BigDecimal("212.41")
        );

        assertTrue(doc.getText().contains("1 diária"));
        assertTrue(doc.getText().contains("Desconto: R$ 37,49"));
        assertTrue(doc.getText().contains("Total: R$ 212,41"));
    }
}
