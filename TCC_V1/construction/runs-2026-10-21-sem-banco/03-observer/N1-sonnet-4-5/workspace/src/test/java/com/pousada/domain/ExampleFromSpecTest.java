package com.pousada.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ExampleFromSpecTest {

    @Test
    void exampleFromSpec_StandardRoom_HighSeason_TwoNights() {
        Reservation reservation = new Reservation(
            1, "101", "João", "joao@example.com", "12345678900",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        Money nightlyRate = new Money(new BigDecimal("249.90"));
        Money subtotal = reservation.calculateSubtotal(nightlyRate);

        assertEquals(2, reservation.getNights());
        assertEquals("599.76", subtotal.getAmount().toString());
    }

    @Test
    void exampleFromSpec_DeluxeRoom_LowSeason_TwoNights() {
        Reservation reservation = new Reservation(
            1, "201", "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        Money nightlyRate = new Money(new BigDecimal("389.90"));
        Money subtotal = reservation.calculateSubtotal(nightlyRate);
        Money discount = reservation.calculateDiscount(subtotal);
        Money total = reservation.calculateTotal(subtotal, discount);

        assertEquals(2, reservation.getNights());
        assertEquals("779.80", subtotal.getAmount().toString());
        assertEquals("0.00", discount.getAmount().toString());
        assertEquals("779.80", total.getAmount().toString());
    }

    @Test
    void exampleFromSpec_NonRefundableDiscount() {
        Reservation reservation = new Reservation(
            1, "201", "Ana", "ana@example.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        Money nightlyRate = new Money(new BigDecimal("389.90"));
        Money subtotal = reservation.calculateSubtotal(nightlyRate);
        Money discount = reservation.calculateDiscount(subtotal);
        Money total = reservation.calculateTotal(subtotal, discount);

        assertEquals("779.80", subtotal.getAmount().toString());
        assertEquals("116.97", discount.getAmount().toString());
        assertEquals("662.83", total.getAmount().toString());
    }

    @Test
    void shouldIdentifyHighSeasonCorrectly() {
        Reservation dec14 = new Reservation(1, "101", "Test", "test@test.com", "111",
            LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 15), 1, RatePlan.STANDARD);

        Reservation dec15 = new Reservation(1, "101", "Test", "test@test.com", "111",
            LocalDate.of(2027, 12, 15), LocalDate.of(2027, 12, 16), 1, RatePlan.STANDARD);

        Reservation jan = new Reservation(1, "101", "Test", "test@test.com", "111",
            LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 11), 1, RatePlan.STANDARD);

        Reservation feb = new Reservation(1, "101", "Test", "test@test.com", "111",
            LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 1), 1, RatePlan.STANDARD);

        Reservation jul = new Reservation(1, "101", "Test", "test@test.com", "111",
            LocalDate.of(2027, 7, 15), LocalDate.of(2027, 7, 16), 1, RatePlan.STANDARD);

        Reservation mar = new Reservation(1, "101", "Test", "test@test.com", "111",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 1, RatePlan.STANDARD);

        Money nightlyRate = new Money(new BigDecimal("249.90"));

        assertEquals("249.90", dec14.calculateSubtotal(nightlyRate).getAmount().toString());
        assertEquals("299.88", dec15.calculateSubtotal(nightlyRate).getAmount().toString());
        assertEquals("299.88", jan.calculateSubtotal(nightlyRate).getAmount().toString());
        assertEquals("299.88", feb.calculateSubtotal(nightlyRate).getAmount().toString());
        assertEquals("299.88", jul.calculateSubtotal(nightlyRate).getAmount().toString());
        assertEquals("249.90", mar.calculateSubtotal(nightlyRate).getAmount().toString());
    }
}
