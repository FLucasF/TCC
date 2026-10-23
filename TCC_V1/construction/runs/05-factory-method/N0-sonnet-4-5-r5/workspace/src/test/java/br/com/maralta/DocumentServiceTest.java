package br.com.maralta;

import br.com.maralta.model.*;
import br.com.maralta.service.DocumentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentServiceTest {

    private final DocumentService documentService = new DocumentService();

    @Test
    void testGenerateReceiptWithCpf() {
        Room room = new Room("201", RoomType.DELUXE);

        Reservation reservation = new Reservation();
        reservation.setId(7L);
        reservation.setGuestName("Carla Dias");
        reservation.setIdentification(GuestIdentification.withCpf("111.222.333-44"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setNights(2);
        reservation.setSubtotal(new BigDecimal("779.80"));
        reservation.setDiscount(BigDecimal.ZERO);
        reservation.setTotal(new BigDecimal("779.80"));

        Document doc = documentService.generateDocument(reservation, room);

        assertEquals("Comprovante de reserva", doc.getTitle());
        assertTrue(doc.getText().contains("Comprovante de reserva nº 7"));
        assertTrue(doc.getText().contains("Carla Dias (CPF 111.222.333-44)"));
        assertTrue(doc.getText().contains("201 (DELUXE)"));
        assertTrue(doc.getText().contains("10/03/2027"));
        assertTrue(doc.getText().contains("12/03/2027"));
        assertTrue(doc.getText().contains("2 diárias"));
        assertTrue(doc.getText().contains("R$ 779,80"));
    }

    @Test
    void testGenerateInvoiceWithCompany() {
        Room room = new Room("201", RoomType.DELUXE);

        Reservation reservation = new Reservation();
        reservation.setId(3L);
        reservation.setGuestName("Ana Souza");
        reservation.setIdentification(GuestIdentification.withCompany("12.345.678/0001-90", "Tecnosul Ltda"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setNights(2);
        reservation.setSubtotal(new BigDecimal("779.80"));
        reservation.setDiscount(BigDecimal.ZERO);
        reservation.setTotal(new BigDecimal("779.80"));

        Document doc = documentService.generateDocument(reservation, room);

        assertEquals("Fatura de hospedagem", doc.getTitle());
        assertTrue(doc.getText().contains("Fatura de hospedagem nº 3"));
        assertTrue(doc.getText().contains("Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(doc.getText().contains("Hóspede: Ana Souza"));
        assertTrue(doc.getText().contains("ISS retido (5%): R$ 38,99"));
    }

    @Test
    void testGenerateBookingReceiptWithPassport() {
        Room room = new Room("201", RoomType.DELUXE);

        Reservation reservation = new Reservation();
        reservation.setId(4L);
        reservation.setGuestName("John Smith");
        reservation.setIdentification(GuestIdentification.withPassport("X1234567", "United Kingdom"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setNights(2);
        reservation.setSubtotal(new BigDecimal("779.80"));
        reservation.setDiscount(BigDecimal.ZERO);
        reservation.setTotal(new BigDecimal("779.80"));

        Document doc = documentService.generateDocument(reservation, room);

        assertEquals("Booking receipt", doc.getTitle());
        assertTrue(doc.getText().contains("Booking receipt no. 4"));
        assertTrue(doc.getText().contains("John Smith (passport X1234567, United Kingdom)"));
        assertTrue(doc.getText().contains("2027-03-10"));
        assertTrue(doc.getText().contains("2027-03-12"));
        assertTrue(doc.getText().contains("2 nights"));
        assertTrue(doc.getText().contains("BRL 779.80"));
    }

    @Test
    void testReceiptWithDiscount() {
        Room room = new Room("201", RoomType.DELUXE);

        Reservation reservation = new Reservation();
        reservation.setId(5L);
        reservation.setGuestName("Maria Silva");
        reservation.setIdentification(GuestIdentification.withCpf("999.888.777-66"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setNights(2);
        reservation.setSubtotal(new BigDecimal("779.80"));
        reservation.setDiscount(new BigDecimal("116.97"));
        reservation.setTotal(new BigDecimal("662.83"));

        Document doc = documentService.generateDocument(reservation, room);

        assertTrue(doc.getText().contains("Desconto: R$ 116,97"));
        assertTrue(doc.getText().contains("Total: R$ 662,83"));
    }

    @Test
    void testReceiptWithOneNight() {
        Room room = new Room("101", RoomType.STANDARD);

        Reservation reservation = new Reservation();
        reservation.setId(6L);
        reservation.setGuestName("Pedro Costa");
        reservation.setIdentification(GuestIdentification.withCpf("123.456.789-00"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 11));
        reservation.setNights(1);
        reservation.setSubtotal(new BigDecimal("249.90"));
        reservation.setDiscount(BigDecimal.ZERO);
        reservation.setTotal(new BigDecimal("249.90"));

        Document doc = documentService.generateDocument(reservation, room);

        assertTrue(doc.getText().contains("1 diária"));
    }
}
