package br.com.pousada;

import br.com.pousada.domain.*;
import br.com.pousada.service.DocumentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentServiceTest {
    private final DocumentService documentService = new DocumentService();

    @Test
    void testGenerateComprovanteWithCpf() {
        Room room = new Room("201", RoomType.DELUXE);
        Reservation reservation = new Reservation(
            7L, "201", "Carla Dias", "carla@example.com",
            new CpfIdentification("11122233344"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD, 2,
            new BigDecimal("779.80"), BigDecimal.ZERO, new BigDecimal("779.80"),
            ReservationStatus.CONFIRMED, new BigDecimal("779.80"), null
        );

        Document document = documentService.generateDocument(reservation, room);

        assertEquals("Comprovante de reserva", document.title());
        assertEquals(7L, document.reservationId());
        assertEquals("Carla Dias", document.guestName());
        assertEquals("11122233344", document.guestCpf());
        assertNull(document.companyCnpj());
        assertNull(document.guestPassport());
        assertTrue(document.text().contains("Comprovante de reserva nº 7"));
        assertTrue(document.text().contains("Hóspede: Carla Dias (CPF 111.222.333-44)"));
        assertTrue(document.text().contains("Quarto: 201 (DELUXE)"));
        assertTrue(document.text().contains("2 diárias"));
        assertTrue(document.text().contains("779,80"));
    }

    @Test
    void testGenerateFaturaWithCnpj() {
        Room room = new Room("201", RoomType.DELUXE);
        Reservation reservation = new Reservation(
            3L, "201", "Ana Souza", "ana@example.com",
            new CnpjIdentification("12345678000190", "Tecnosul Ltda"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD, 2,
            new BigDecimal("779.80"), BigDecimal.ZERO, new BigDecimal("779.80"),
            ReservationStatus.CONFIRMED, new BigDecimal("779.80"), null
        );

        Document document = documentService.generateDocument(reservation, room);

        assertEquals("Fatura de hospedagem", document.title());
        assertEquals("12345678000190", document.companyCnpj());
        assertEquals("Tecnosul Ltda", document.companyName());
        assertNull(document.guestCpf());
        assertTrue(document.text().contains("Fatura de hospedagem nº 3"));
        assertTrue(document.text().contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(document.text().contains("Hóspede: Ana Souza"));
        assertTrue(document.text().contains("ISS retido (5%)"));
        assertTrue(document.text().contains("38,99"));
    }

    @Test
    void testGenerateReceiptWithPassport() {
        Room room = new Room("201", RoomType.DELUXE);
        Reservation reservation = new Reservation(
            4L, "201", "John Smith", "john@example.com",
            new PassportIdentification("X1234567", "United Kingdom"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD, 2,
            new BigDecimal("779.80"), BigDecimal.ZERO, new BigDecimal("779.80"),
            ReservationStatus.CONFIRMED, new BigDecimal("779.80"), null
        );

        Document document = documentService.generateDocument(reservation, room);

        assertEquals("Booking receipt", document.title());
        assertEquals("X1234567", document.guestPassport());
        assertEquals("United Kingdom", document.guestCountry());
        assertNull(document.guestCpf());
        assertNull(document.companyCnpj());
        assertTrue(document.text().contains("Booking receipt no. 4"));
        assertTrue(document.text().contains("Guest: John Smith (passport X1234567, United Kingdom)"));
        assertTrue(document.text().contains("2 nights"));
        assertTrue(document.text().contains("BRL 779.80"));
        assertTrue(document.text().contains("Check-in: 2027-03-10"));
    }

    @Test
    void testDocumentWithDiscount() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = new Reservation(
            5L, "101", "Maria Santos", "maria@example.com",
            new CpfIdentification("98765432100"),
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            1, RatePlan.NON_REFUNDABLE, 2,
            new BigDecimal("599.76"), new BigDecimal("89.96"), new BigDecimal("509.80"),
            ReservationStatus.CONFIRMED, new BigDecimal("509.80"), null
        );

        Document document = documentService.generateDocument(reservation, room);

        assertTrue(document.text().contains("Diárias"));
        assertTrue(document.text().contains("599,76"));
        assertTrue(document.text().contains("Desconto"));
        assertTrue(document.text().contains("89,96"));
        assertTrue(document.text().contains("Total"));
        assertTrue(document.text().contains("509,80"));
    }

    @Test
    void testDocumentSingleNight() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = new Reservation(
            6L, "101", "Pedro Lima", "pedro@example.com",
            new CpfIdentification("11111111111"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 11),
            1, RatePlan.STANDARD, 1,
            new BigDecimal("249.90"), BigDecimal.ZERO, new BigDecimal("249.90"),
            ReservationStatus.CONFIRMED, new BigDecimal("249.90"), null
        );

        Document document = documentService.generateDocument(reservation, room);

        assertTrue(document.text().contains("1 diária"));
        assertFalse(document.text().contains("2 diárias"));
    }
}
