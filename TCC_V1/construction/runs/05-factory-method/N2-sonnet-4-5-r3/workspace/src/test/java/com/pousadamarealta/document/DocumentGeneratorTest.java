package com.pousadamarealta.document;

import com.pousadamarealta.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DocumentGeneratorTest {

    @Test
    void testIndividualDocumentGenerator() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setGuestName("João Silva");
        reservation.setGuestCpf("111.222.333-44");
        reservation.setCheckIn(LocalDate.of(2027, 7, 10));
        reservation.setCheckOut(LocalDate.of(2027, 7, 12));
        reservation.setRatePlan(RatePlan.STANDARD);

        Room room = new Room("101", RoomType.STANDARD);

        DocumentGenerator generator = new IndividualDocumentGenerator();
        Document document = generator.generate(reservation, room);

        assertEquals("Comprovante de reserva", document.getTitle());
        assertEquals(1L, document.getReservationId());
        assertEquals("João Silva", document.getGuestName());
        assertEquals("111.222.333-44", document.getGuestCpf());
        assertNull(document.getCompanyCnpj());
        assertNull(document.getGuestPassport());
        assertEquals(2, document.getNights());
        assertTrue(document.getText().contains("Comprovante de reserva nº 1"));
        assertTrue(document.getText().contains("CPF 111.222.333-44"));
        assertTrue(document.getText().contains("R$ 599,76"));
    }

    @Test
    void testCorporateDocumentGenerator() {
        Reservation reservation = new Reservation();
        reservation.setId(2L);
        reservation.setGuestName("Ana Souza");
        reservation.setCompanyCnpj("12.345.678/0001-90");
        reservation.setCompanyName("Tecnosul Ltda");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setRatePlan(RatePlan.NON_REFUNDABLE);

        Room room = new Room("201", RoomType.DELUXE);

        DocumentGenerator generator = new CorporateDocumentGenerator();
        Document document = generator.generate(reservation, room);

        assertEquals("Fatura de hospedagem", document.getTitle());
        assertEquals(2L, document.getReservationId());
        assertEquals("Ana Souza", document.getGuestName());
        assertEquals("Tecnosul Ltda", document.getCompanyName());
        assertNull(document.getGuestCpf());
        assertNull(document.getGuestPassport());
        assertTrue(document.getText().contains("Fatura de hospedagem nº 2"));
        assertTrue(document.getText().contains("CNPJ 12.345.678/0001-90"));
        assertTrue(document.getText().contains("ISS retido (5%)"));
    }

    @Test
    void testInternationalDocumentGenerator() {
        Reservation reservation = new Reservation();
        reservation.setId(3L);
        reservation.setGuestName("John Smith");
        reservation.setGuestPassport("X1234567");
        reservation.setGuestCountry("United Kingdom");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setRatePlan(RatePlan.STANDARD);

        Room room = new Room("201", RoomType.DELUXE);

        DocumentGenerator generator = new InternationalDocumentGenerator();
        Document document = generator.generate(reservation, room);

        assertEquals("Booking receipt", document.getTitle());
        assertEquals(3L, document.getReservationId());
        assertEquals("John Smith", document.getGuestName());
        assertEquals("X1234567", document.getGuestPassport());
        assertEquals("United Kingdom", document.getGuestCountry());
        assertNull(document.getGuestCpf());
        assertNull(document.getCompanyCnpj());
        assertTrue(document.getText().contains("Booking receipt no. 3"));
        assertTrue(document.getText().contains("passport X1234567"));
        assertTrue(document.getText().contains("United Kingdom"));
        assertTrue(document.getText().contains("BRL 779.80"));
        assertTrue(document.getText().contains("2027-03-10"));
    }

    @Test
    void testDocumentGeneratorFactory() {
        DocumentGeneratorFactory factory = new DocumentGeneratorFactory();

        Reservation individual = new Reservation();
        individual.setGuestCpf("111.222.333-44");
        assertTrue(factory.getGenerator(individual) instanceof IndividualDocumentGenerator);

        Reservation corporate = new Reservation();
        corporate.setCompanyCnpj("12.345.678/0001-90");
        corporate.setCompanyName("Tecnosul Ltda");
        assertTrue(factory.getGenerator(corporate) instanceof CorporateDocumentGenerator);

        Reservation international = new Reservation();
        international.setGuestPassport("X1234567");
        international.setGuestCountry("United Kingdom");
        assertTrue(factory.getGenerator(international) instanceof InternationalDocumentGenerator);
    }

    @Test
    void testDocumentWithDiscount() {
        Reservation reservation = new Reservation();
        reservation.setId(4L);
        reservation.setGuestName("Maria Santos");
        reservation.setGuestCpf("555.666.777-88");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setRatePlan(RatePlan.NON_REFUNDABLE);

        Room room = new Room("201", RoomType.DELUXE);

        DocumentGenerator generator = new IndividualDocumentGenerator();
        Document document = generator.generate(reservation, room);

        assertTrue(document.getText().contains("Desconto: R$ 116,97"));
        assertTrue(document.getText().contains("Total: R$ 662,83"));
    }

    @Test
    void testDocumentSingleNight() {
        Reservation reservation = new Reservation();
        reservation.setId(5L);
        reservation.setGuestName("Pedro Lima");
        reservation.setGuestCpf("999.888.777-66");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 11));
        reservation.setRatePlan(RatePlan.STANDARD);

        Room room = new Room("101", RoomType.STANDARD);

        DocumentGenerator generator = new IndividualDocumentGenerator();
        Document document = generator.generate(reservation, room);

        assertEquals(1, document.getNights());
        assertTrue(document.getText().contains("1 diária"));

        DocumentGenerator intlGenerator = new InternationalDocumentGenerator();
        reservation.setGuestCpf(null);
        reservation.setGuestPassport("Y7654321");
        reservation.setGuestCountry("Canada");
        Document intlDocument = intlGenerator.generate(reservation, room);

        assertTrue(intlDocument.getText().contains("1 night"));
    }
}
