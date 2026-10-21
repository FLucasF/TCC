package com.pousada.document;

import com.pousada.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DocumentGeneratorTest {

    @Test
    void testComprovanteGeneration() {
        Reservation reservation = new Reservation(
            7,
            "201",
            RoomType.DELUXE,
            "Carla Dias",
            "carla@email.com",
            new CpfIdentification("11122233344"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        DocumentGenerator generator = new ComprovanteGenerator();
        String result = generator.generate(reservation);

        assertTrue(result.contains("Comprovante de reserva nº 7"));
        assertTrue(result.contains("Hóspede: Carla Dias (CPF 111.222.333-44)"));
        assertTrue(result.contains("Quarto: 201 (DELUXE)"));
        assertTrue(result.contains("Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias"));
        assertTrue(result.contains("Diárias: R$ 779,80"));
        assertTrue(result.contains("Total: R$ 779,80"));
        assertFalse(result.contains("Desconto"));
    }

    @Test
    void testComprovanteWithDiscount() {
        Reservation reservation = new Reservation(
            1,
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@email.com",
            new CpfIdentification("12345678901"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 11),
            1,
            RatePlan.NON_REFUNDABLE,
            1,
            new BigDecimal("249.90"),
            new BigDecimal("37.49"),
            new BigDecimal("212.41")
        );

        DocumentGenerator generator = new ComprovanteGenerator();
        String result = generator.generate(reservation);

        assertTrue(result.contains("Desconto: R$ 37,49"));
        assertTrue(result.contains("Total: R$ 212,41"));
        assertTrue(result.contains("1 diária"));
    }

    @Test
    void testFaturaGeneration() {
        Reservation reservation = new Reservation(
            3,
            "201",
            RoomType.DELUXE,
            "Ana Souza",
            "ana@empresa.com",
            new CnpjIdentification("12345678000190", "Tecnosul Ltda"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        DocumentGenerator generator = new FaturaGenerator();
        String result = generator.generate(reservation);

        assertTrue(result.contains("Fatura de hospedagem nº 3"));
        assertTrue(result.contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(result.contains("Hóspede: Ana Souza"));
        assertTrue(result.contains("ISS retido (5%): R$ 38,99"));
    }

    @Test
    void testReceiptGeneration() {
        Reservation reservation = new Reservation(
            4,
            "201",
            RoomType.DELUXE,
            "John Smith",
            "john@email.com",
            new PassportIdentification("X1234567", "United Kingdom"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            2,
            new BigDecimal("779.80"),
            BigDecimal.ZERO,
            new BigDecimal("779.80")
        );

        DocumentGenerator generator = new ReceiptGenerator();
        String result = generator.generate(reservation);

        assertTrue(result.contains("Booking receipt no. 4"));
        assertTrue(result.contains("Guest: John Smith (passport X1234567, United Kingdom)"));
        assertTrue(result.contains("Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights"));
        assertTrue(result.contains("Nights: BRL 779.80"));
        assertTrue(result.contains("Total: BRL 779.80"));
    }

    @Test
    void testReceiptWithDiscount() {
        Reservation reservation = new Reservation(
            5,
            "301",
            RoomType.SUITE,
            "Jane Doe",
            "jane@email.com",
            new PassportIdentification("Y7654321", "United States"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 11),
            3,
            RatePlan.NON_REFUNDABLE,
            1,
            new BigDecimal("579.90"),
            new BigDecimal("86.99"),
            new BigDecimal("492.91")
        );

        DocumentGenerator generator = new ReceiptGenerator();
        String result = generator.generate(reservation);

        assertTrue(result.contains("Discount: BRL 86.99"));
        assertTrue(result.contains("Total: BRL 492.91"));
        assertTrue(result.contains("1 night"));
    }
}
