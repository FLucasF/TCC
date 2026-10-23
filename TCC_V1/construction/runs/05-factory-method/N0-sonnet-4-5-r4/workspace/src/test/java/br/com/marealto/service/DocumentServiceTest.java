package br.com.marealto.service;

import br.com.marealto.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DocumentServiceTest {
    private DocumentService documentService;
    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
        documentService = new DocumentService(pricingService);
    }

    @Test
    void testGenerateReceiptForIndividual() {
        Reservation reservation = new Reservation();
        reservation.setId(7L);
        reservation.setRoomNumber("201");
        reservation.setGuestName("Carla Dias");
        reservation.setGuestEmail("carla@example.com");
        reservation.setGuestIdentification(new GuestIdentification.Individual("11122233344"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setNights(2);
        reservation.setSubtotal(new BigDecimal("779.80"));
        reservation.setDiscount(new BigDecimal("0.00"));
        reservation.setTotal(new BigDecimal("779.80"));

        String document = documentService.generateDocument(reservation, RoomType.DELUXE);

        assertTrue(document.contains("Comprovante de reserva nº 7"));
        assertTrue(document.contains("Carla Dias"));
        assertTrue(document.contains("111.222.333-44"));
        assertTrue(document.contains("201 (DELUXE)"));
        assertTrue(document.contains("10/03/2027"));
        assertTrue(document.contains("12/03/2027"));
        assertTrue(document.contains("2 diárias"));
        assertTrue(document.contains("R$"));
        assertTrue(document.contains("779,80"));
    }

    @Test
    void testGenerateInvoiceForCompany() {
        Reservation reservation = new Reservation();
        reservation.setId(3L);
        reservation.setRoomNumber("201");
        reservation.setGuestName("Ana Souza");
        reservation.setGuestEmail("ana@tecnosul.com");
        reservation.setGuestIdentification(new GuestIdentification.Company("12345678000190", "Tecnosul Ltda"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setNights(2);
        reservation.setSubtotal(new BigDecimal("779.80"));
        reservation.setDiscount(new BigDecimal("0.00"));
        reservation.setTotal(new BigDecimal("779.80"));

        String document = documentService.generateDocument(reservation, RoomType.DELUXE);

        assertTrue(document.contains("Fatura de hospedagem nº 3"));
        assertTrue(document.contains("Tecnosul Ltda"));
        assertTrue(document.contains("12.345.678/0001-90"));
        assertTrue(document.contains("Ana Souza"));
        assertTrue(document.contains("ISS retido (5%)"));
        assertTrue(document.contains("38,99"));
    }

    @Test
    void testGenerateBookingReceiptForForeign() {
        Reservation reservation = new Reservation();
        reservation.setId(4L);
        reservation.setRoomNumber("201");
        reservation.setGuestName("John Smith");
        reservation.setGuestEmail("john@example.com");
        reservation.setGuestIdentification(new GuestIdentification.Foreign("X1234567", "United Kingdom"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setNights(2);
        reservation.setSubtotal(new BigDecimal("779.80"));
        reservation.setDiscount(new BigDecimal("0.00"));
        reservation.setTotal(new BigDecimal("779.80"));

        String document = documentService.generateDocument(reservation, RoomType.DELUXE);

        assertTrue(document.contains("Booking receipt no. 4"));
        assertTrue(document.contains("John Smith"));
        assertTrue(document.contains("passport X1234567"));
        assertTrue(document.contains("United Kingdom"));
        assertTrue(document.contains("2027-03-10"));
        assertTrue(document.contains("2027-03-12"));
        assertTrue(document.contains("2 nights"));
        assertTrue(document.contains("BRL 779.80"));
    }

    @Test
    void testGenerateConfirmationEmail() {
        Reservation reservation = new Reservation();
        reservation.setId(7L);
        reservation.setRoomNumber("201");
        reservation.setGuestName("Carla Dias");
        reservation.setGuestEmail("carla@example.com");
        reservation.setGuestIdentification(new GuestIdentification.Individual("11122233344"));
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setNights(2);
        reservation.setSubtotal(new BigDecimal("779.80"));
        reservation.setDiscount(new BigDecimal("0.00"));
        reservation.setTotal(new BigDecimal("779.80"));

        String email = documentService.generateConfirmationEmail(reservation, RoomType.DELUXE);

        assertTrue(email.contains("Olá, Carla Dias!"));
        assertTrue(email.contains("Sua reserva na Pousada Maré Alta está confirmada"));
        assertTrue(email.contains("Comprovante de reserva nº 7"));
        assertTrue(email.contains("A entrada é a partir das 14h"));
        assertTrue(email.contains("Até breve!"));
    }
}
