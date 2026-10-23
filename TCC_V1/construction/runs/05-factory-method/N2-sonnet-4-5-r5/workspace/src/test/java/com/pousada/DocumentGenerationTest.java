package com.pousada;

import com.pousada.domain.guest.CorporateGuest;
import com.pousada.domain.guest.ForeignGuest;
import com.pousada.domain.guest.IndividualGuest;
import com.pousada.domain.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentGenerationTest {

    @Test
    void generatesIndividualGuestDocument() {
        Reservation reservation = createReservation(
            new IndividualGuest("111.222.333-44"),
            "Carla Dias",
            RoomType.DELUXE,
            "201",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12)
        );

        Document document = reservation.generateDocument();

        assertThat(document.title()).isEqualTo("Comprovante de reserva");
        assertThat(document.guestCpf()).isEqualTo("111.222.333-44");
        assertThat(document.text()).contains("Comprovante de reserva nº 7");
        assertThat(document.text()).contains("Hóspede: Carla Dias (CPF 111.222.333-44)");
        assertThat(document.text()).contains("Quarto: 201 (DELUXE)");
        assertThat(document.text()).contains("Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias");
        assertThat(document.text()).contains("779,80");
    }

    @Test
    void generatesCorporateGuestDocument() {
        Reservation reservation = createReservation(
            new CorporateGuest("12.345.678/0001-90", "Tecnosul Ltda"),
            "Ana Souza",
            RoomType.DELUXE,
            "201",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12)
        );

        Document document = reservation.generateDocument();

        assertThat(document.title()).isEqualTo("Fatura de hospedagem");
        assertThat(document.companyCnpj()).isEqualTo("12.345.678/0001-90");
        assertThat(document.companyName()).isEqualTo("Tecnosul Ltda");
        assertThat(document.text()).contains("Fatura de hospedagem nº 3");
        assertThat(document.text()).contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)");
        assertThat(document.text()).contains("Hóspede: Ana Souza");
        assertThat(document.text()).contains("ISS retido (5%)");
        assertThat(document.text()).contains("38,99");
    }

    @Test
    void generatesForeignGuestDocument() {
        Reservation reservation = createReservation(
            new ForeignGuest("X1234567", "United Kingdom"),
            "John Smith",
            RoomType.DELUXE,
            "201",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12)
        );

        Document document = reservation.generateDocument();

        assertThat(document.title()).isEqualTo("Booking receipt");
        assertThat(document.guestPassport()).isEqualTo("X1234567");
        assertThat(document.guestCountry()).isEqualTo("United Kingdom");
        assertThat(document.text()).contains("Booking receipt no. 4");
        assertThat(document.text()).contains("Guest: John Smith (passport X1234567, United Kingdom)");
        assertThat(document.text()).contains("Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights");
        assertThat(document.text()).contains("BRL 779.80");
    }

    private Reservation createReservation(Object guestType, String guestName, RoomType roomType,
                                         String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return Reservation.builder()
            .id(guestType instanceof IndividualGuest ? 7 :
                guestType instanceof CorporateGuest ? 3 : 4)
            .roomNumber(roomNumber)
            .roomType(roomType)
            .guestName(guestName)
            .guestEmail("test@example.com")
            .guestType((com.pousada.domain.guest.GuestType) guestType)
            .checkIn(checkIn)
            .checkOut(checkOut)
            .guests(2)
            .ratePlan(RatePlan.STANDARD)
            .nights(2)
            .subtotal(new BigDecimal("779.80"))
            .discount(BigDecimal.ZERO)
            .total(new BigDecimal("779.80"))
            .build();
    }
}
