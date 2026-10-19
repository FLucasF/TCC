package com.pousadamareaalta.service;

import com.pousadamareaalta.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class DocumentServiceTest {
    private final DocumentService documentService = new DocumentService();

    @Test
    void shouldGenerateCpfComprovante() {
        Reservation reservation = new Reservation(
            7,
            201,
            "Carla Dias",
            "carla@example.com",
            "111.222.333-44",
            null,
            null,
            null,
            null,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        Room room = new Room(201, Room.RoomType.DELUXE, 3, new BigDecimal("389.90"));
        Document doc = documentService.generateDocument(reservation, room);

        assertThat(doc.title()).isEqualTo("Comprovante de reserva");
        assertThat(doc.text()).contains("Comprovante de reserva nº 7");
        assertThat(doc.text()).contains("Hóspede: Carla Dias (CPF 111.222.333-44)");
        assertThat(doc.text()).contains("Quarto: 201 (DELUXE)");
        assertThat(doc.text()).contains("Entrada: 10/03/2027");
        assertThat(doc.text()).contains("2 diárias");
    }

    @Test
    void shouldGenerateCnpjFatura() {
        Reservation reservation = new Reservation(
            3,
            201,
            "Ana Souza",
            "ana@example.com",
            null,
            "12.345.678/0001-90",
            "Tecnosul Ltda",
            null,
            null,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            1,
            RatePlan.STANDARD,
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        Room room = new Room(201, Room.RoomType.DELUXE, 3, new BigDecimal("389.90"));
        Document doc = documentService.generateDocument(reservation, room);

        assertThat(doc.title()).isEqualTo("Fatura de hospedagem");
        assertThat(doc.text()).contains("Fatura de hospedagem nº 3");
        assertThat(doc.text()).contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)");
        assertThat(doc.text()).contains("Hóspede: Ana Souza");
        assertThat(doc.text()).contains("ISS retido (5%):");
    }

    @Test
    void shouldGenerateBookingReceipt() {
        Reservation reservation = new Reservation(
            4,
            201,
            "John Smith",
            "john@example.com",
            null,
            null,
            null,
            "X1234567",
            "United Kingdom",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            1,
            RatePlan.STANDARD,
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        Room room = new Room(201, Room.RoomType.DELUXE, 3, new BigDecimal("389.90"));
        Document doc = documentService.generateDocument(reservation, room);

        assertThat(doc.title()).isEqualTo("Booking receipt");
        assertThat(doc.text()).contains("Booking receipt no. 4");
        assertThat(doc.text()).contains("Guest: John Smith (passport X1234567, United Kingdom)");
        assertThat(doc.text()).contains("Check-in: 2027-03-10");
        assertThat(doc.text()).contains("2 nights");
        assertThat(doc.text()).contains("BRL");
    }

    @Test
    void shouldFormatCurrencyCorrectly() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "123.456.789-00",
            null,
            null,
            null,
            null,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 11),
            1,
            RatePlan.STANDARD,
            1,
            new BigDecimal("249.90"),
            BigDecimal.ZERO,
            new BigDecimal("249.90")
        );

        Room room = new Room(101, Room.RoomType.STANDARD, 2, new BigDecimal("249.90"));
        Document doc = documentService.generateDocument(reservation, room);

        assertThat(doc.text()).contains("R$ 249,90");
    }

    @Test
    void shouldIncludeDiscountLineWhenPresent() {
        Reservation reservation = new Reservation(
            1,
            201,
            "Jane Doe",
            "jane@example.com",
            "111.222.333-44",
            null,
            null,
            null,
            null,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            1,
            RatePlan.NON_REFUNDABLE,
            2,
            new BigDecimal("779.80"),
            new BigDecimal("116.97"),
            new BigDecimal("662.83")
        );

        Room room = new Room(201, Room.RoomType.DELUXE, 3, new BigDecimal("389.90"));
        Document doc = documentService.generateDocument(reservation, room);

        assertThat(doc.text()).contains("Desconto:");
    }

    @Test
    void shouldHandleSingleNight() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "123.456.789-00",
            null,
            null,
            null,
            null,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 11),
            1,
            RatePlan.STANDARD,
            1,
            new BigDecimal("249.90"),
            BigDecimal.ZERO,
            new BigDecimal("249.90")
        );

        Room room = new Room(101, Room.RoomType.STANDARD, 2, new BigDecimal("249.90"));
        Document doc = documentService.generateDocument(reservation, room);

        assertThat(doc.text()).contains("1 diária");
    }
}
