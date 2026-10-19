package com.pousadamarealta;

import com.pousadamarealta.model.*;
import com.pousadamarealta.util.DocumentFormatter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentFormatterTest {

    @Test
    void shouldFormatReceipt() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");
        Reservation reservation = new Reservation(
            7,
            "201",
            "Carla Dias",
            "carla@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        Room room = new Room("201", RoomType.DELUXE, 3, new BigDecimal("389.90"));

        String receipt = DocumentFormatter.formatReceipt(reservation, room);

        assertThat(receipt).contains("Comprovante de reserva nº 7");
        assertThat(receipt).contains("Hóspede: Carla Dias (CPF 111.222.333-44)");
        assertThat(receipt).contains("Quarto: 201 (DELUXE)");
        assertThat(receipt).contains("Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias");
        assertThat(receipt).contains("Diárias: R$ 779,80");
        assertThat(receipt).contains("Total: R$ 779,80");
        assertThat(receipt).doesNotContain("Desconto");
    }

    @Test
    void shouldFormatInvoice() {
        GuestIdentification id = GuestIdentification.withCompany("12.345.678/0001-90", "Tecnosul Ltda");
        Reservation reservation = new Reservation(
            3,
            "201",
            "Ana Souza",
            "ana@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        Room room = new Room("201", RoomType.DELUXE, 3, new BigDecimal("389.90"));

        String invoice = DocumentFormatter.formatInvoice(reservation, room);

        assertThat(invoice).contains("Fatura de hospedagem nº 3");
        assertThat(invoice).contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)");
        assertThat(invoice).contains("Hóspede: Ana Souza");
        assertThat(invoice).contains("ISS retido (5%): R$ 38,99");
    }

    @Test
    void shouldFormatBookingReceipt() {
        GuestIdentification id = GuestIdentification.withPassport("X1234567", "United Kingdom");
        Reservation reservation = new Reservation(
            4,
            "201",
            "John Smith",
            "john@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        Room room = new Room("201", RoomType.DELUXE, 3, new BigDecimal("389.90"));

        String receipt = DocumentFormatter.formatBookingReceipt(reservation, room);

        assertThat(receipt).contains("Booking receipt no. 4");
        assertThat(receipt).contains("Guest: John Smith (passport X1234567, United Kingdom)");
        assertThat(receipt).contains("Room: 201 (DELUXE)");
        assertThat(receipt).contains("Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights");
        assertThat(receipt).contains("Nights: BRL 779.80");
        assertThat(receipt).contains("Total: BRL 779.80");
    }

    @Test
    void shouldFormatConfirmationEmail() {
        String email = DocumentFormatter.formatConfirmationEmail("Carla Dias", "Document text here");

        assertThat(email).contains("Olá, Carla Dias!");
        assertThat(email).contains("Sua reserva na Pousada Maré Alta está confirmada");
        assertThat(email).contains("Document text here");
        assertThat(email).contains("A entrada é a partir das 14h");
    }
}
