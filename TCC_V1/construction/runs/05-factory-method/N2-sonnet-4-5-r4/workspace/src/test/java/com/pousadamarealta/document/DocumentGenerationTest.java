package com.pousadamarealta.document;

import com.pousadamarealta.domain.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentGenerationTest {

    private final DocumentService documentService = new DocumentService();

    @Test
    void shouldGenerateIndividualGuestDocument() {
        Reservation reservation = new Reservation(
            7, 201, "Carla Dias",
            "carla@example.com",
            new GuestIdentification.IndividualGuest("111.222.333-44"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("389.90")
        );
        reservation.confirm();

        Document document = documentService.generateDocument(reservation, RoomType.DELUXE);

        assertThat(document.text()).contains("Comprovante de reserva nº 7");
        assertThat(document.text()).contains("Carla Dias");
        assertThat(document.text()).contains("CPF 111.222.333-44");
        assertThat(document.text()).contains("201 (DELUXE)");
        assertThat(document.text()).contains("10/03/2027");
        assertThat(document.text()).contains("12/03/2027");
        assertThat(document.text()).contains("2 diárias");
        assertThat(document.text()).contains("779,80");
        assertThat(document.text()).contains("R$");
        assertThat(document.text()).doesNotContain("ISS");
        assertThat(document.text()).doesNotContain("BRL");
    }

    @Test
    void shouldGenerateCorporateGuestDocument() {
        Reservation reservation = new Reservation(
            3, 201, "Ana Souza",
            "ana@example.com",
            new GuestIdentification.CorporateGuest("12.345.678/0001-90", "Tecnosul Ltda"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("389.90")
        );
        reservation.confirm();

        Document document = documentService.generateDocument(reservation, RoomType.DELUXE);

        assertThat(document.text()).contains("Fatura de hospedagem nº 3");
        assertThat(document.text()).contains("Tecnosul Ltda");
        assertThat(document.text()).contains("CNPJ 12.345.678/0001-90");
        assertThat(document.text()).contains("Hóspede: Ana Souza");
        assertThat(document.text()).contains("ISS retido (5%)");
        assertThat(document.text()).contains("38,99");
        assertThat(document.text()).contains("R$");
        assertThat(document.text()).doesNotContain("BRL");
    }

    @Test
    void shouldGenerateForeignGuestDocument() {
        Reservation reservation = new Reservation(
            4, 201, "John Smith",
            "john@example.com",
            new GuestIdentification.ForeignGuest("X1234567", "United Kingdom"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("389.90")
        );
        reservation.confirm();

        Document document = documentService.generateDocument(reservation, RoomType.DELUXE);

        assertThat(document.text()).contains("Booking receipt no. 4");
        assertThat(document.text()).contains("John Smith");
        assertThat(document.text()).contains("passport X1234567");
        assertThat(document.text()).contains("United Kingdom");
        assertThat(document.text()).contains("2027-03-10");
        assertThat(document.text()).contains("2027-03-12");
        assertThat(document.text()).contains("2 nights");
        assertThat(document.text()).contains("BRL 779.80");
        assertThat(document.text()).doesNotContain("R$");
        assertThat(document.text()).doesNotContain("ISS");
    }

    @Test
    void shouldGenerateDocumentWithDiscount() {
        Reservation reservation = new Reservation(
            5, 201, "Pedro Santos",
            "pedro@example.com",
            new GuestIdentification.IndividualGuest("222.333.444-55"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE,
            new BigDecimal("389.90")
        );
        reservation.confirm();

        Document document = documentService.generateDocument(reservation, RoomType.DELUXE);

        assertThat(document.text()).contains("Desconto:");
        assertThat(document.text()).contains("116,97");
        assertThat(document.text()).contains("R$");
    }

    @Test
    void shouldGenerateDocumentForSingleNight() {
        Reservation reservation = new Reservation(
            6, 101, "Maria Silva",
            "maria@example.com",
            new GuestIdentification.IndividualGuest("333.444.555-66"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 11),
            1, RatePlan.STANDARD,
            new BigDecimal("249.90")
        );
        reservation.confirm();

        Document document = documentService.generateDocument(reservation, RoomType.STANDARD);

        assertThat(document.text()).contains("1 diária");
        assertThat(document.text()).doesNotContain("diárias");
    }
}
