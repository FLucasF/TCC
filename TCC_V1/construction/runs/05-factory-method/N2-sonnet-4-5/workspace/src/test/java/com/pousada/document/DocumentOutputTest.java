package com.pousada.document;

import com.pousada.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

class DocumentOutputTest {

    @Test
    void printComprovanteOutput() {
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

        System.out.println("===== COMPROVANTE OUTPUT =====");
        System.out.println(result);
        System.out.println("===== END OUTPUT =====");
    }

    @Test
    void printFaturaOutput() {
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

        System.out.println("===== FATURA OUTPUT =====");
        System.out.println(result);
        System.out.println("===== END OUTPUT =====");
    }

    @Test
    void printReceiptOutput() {
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

        System.out.println("===== RECEIPT OUTPUT =====");
        System.out.println(result);
        System.out.println("===== END OUTPUT =====");
    }
}
