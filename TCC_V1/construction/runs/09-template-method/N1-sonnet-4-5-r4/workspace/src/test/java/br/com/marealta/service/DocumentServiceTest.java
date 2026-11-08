package br.com.marealta.service;

import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentServiceTest {
    private final DocumentService documentService = new DocumentService();

    @Test
    void shouldGenerateReceiptWithDiscount() {
        Reservation reservation = new Reservation();
        reservation.setId(7L);
        reservation.setGuestName("Carla Dias");
        reservation.setGuestCpf("11122233344");
        reservation.setRoomNumber("201");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setNights(2);
        reservation.setSubtotal(new BigDecimal("779.80"));
        reservation.setDiscount(new BigDecimal("116.97"));
        reservation.setTotal(new BigDecimal("662.83"));

        String receipt = documentService.generateReceiptText(reservation);

        assertThat(receipt).contains("Comprovante de reserva nº 7");
        assertThat(receipt).contains("Hóspede: Carla Dias (CPF 111.222.333-44)");
        assertThat(receipt).contains("Quarto: 201 (DELUXE)");
        assertThat(receipt).contains("Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias");
        assertThat(receipt).contains("Diárias: R$ 779.80");
        assertThat(receipt).contains("Desconto: R$ 116.97");
        assertThat(receipt).contains("Total: R$ 662.83");
    }

    @Test
    void shouldGenerateReceiptWithoutDiscount() {
        Reservation reservation = new Reservation();
        reservation.setId(7L);
        reservation.setGuestName("Carla Dias");
        reservation.setGuestCpf("11122233344");
        reservation.setRoomNumber("201");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setNights(2);
        reservation.setSubtotal(new BigDecimal("779.80"));
        reservation.setDiscount(BigDecimal.ZERO);
        reservation.setTotal(new BigDecimal("779.80"));

        String receipt = documentService.generateReceiptText(reservation);

        assertThat(receipt).contains("Comprovante de reserva nº 7");
        assertThat(receipt).doesNotContain("Desconto:");
        assertThat(receipt).contains("Total: R$ 779.80");
    }

    @Test
    void shouldUseCorrectPluralForOneNight() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setGuestName("João Silva");
        reservation.setGuestCpf("12345678900");
        reservation.setRoomNumber("101");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 11));
        reservation.setNights(1);
        reservation.setSubtotal(new BigDecimal("249.90"));
        reservation.setDiscount(BigDecimal.ZERO);
        reservation.setTotal(new BigDecimal("249.90"));

        String receipt = documentService.generateReceiptText(reservation);

        assertThat(receipt).contains("1 diária");
        assertThat(receipt).doesNotContain("diárias");
    }
}
